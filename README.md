# Restaurant DDD Model

A Java 21 / Spring Boot modular monolith for modeling a Central Hub and isolated restaurant branches. PostgreSQL stores central configuration and branch-owned prices, orders, payments, and reports. The project is focused on explainable domain rules rather than distributed infrastructure.

## 1. Problem statement

The Central Hub creates branches, centrally managed mastermenu data, and branch-specific availability. Each branch can see only the menu masters explicitly enabled for it. Branch prices vary by branch and customer type (STAFF/GUEST). Orders keep the price used at purchase time; payments belong to an order in the same branch. Central reports aggregate branch-owned transaction data.

## 2. Business rules

- Branch code is normalized and unique.
- A master is available to a branch only through an enabled `BranchMasterAssignment` for that branch.
- Menu lookups, price lookups, orders, payments, and reports include the relevant `branchId`.
- A price is identified by branch, variant, and customer type; database uniqueness prevents duplicates.
- An order item stores its purchase-time `unitPrice` and `lineTotal`.
- A payment is accepted only when its order belongs to the branch in the request.

## 3. Architecture

```text
HTTP request
     |
     v
REST controller (inbound adapter)
     |
     v
RestaurantUseCases (input port)
     |
     v
RestaurantService (application service)
     |                    \
     v                     v
Domain rules       RestaurantPersistencePort (output port)
                           |
                           v
                   JpaRestaurantAdapter
                           |
                           v
                     PostgreSQL
```

This is a simple Clean Architecture + Hexagonal Architecture hybrid in a single deployable application. The controller depends on an application interface. The application service coordinates use cases and depends on an output interface. Infrastructure implements that output interface. The domain package does not import Spring, JDBC, JPA, HTTP, or database classes.

## 4. DDD concepts

- `Branch`: branch identity and normalized business code.
- `MasterData`: centrally owned configuration, stored separately from menu structure.
- `BranchMasterAssignment`: explicit branch-to-master enablement relationship.
- `Menu`, category, item, and variant: flexible menu hierarchy.
- `BranchPrice`: price for one branch, variant, and customer type.
- `RestaurantOrder` and `OrderItem`: order aggregate with historical price snapshots.
- `Payment`: a transaction owned by a branch and order.

A modular monolith means these business areas are separated in code while sharing one application process and database. They are not separately deployed services.

`branch_price` is intentionally keyed by `(branch_id, menu_variant_id, customer_type)`. It stores separate `GUEST` (regular customer) and `STAFF` amounts per branch. The database has a unique constraint for that key, and the application only creates/updates a price when the variant's master is enabled for that branch. An order copies the chosen amount into its order line so later price edits do not alter history.

## 5. Ports and adapters

A **port** is an interface that states what a layer needs or offers. `RestaurantUseCases` is the inbound port: HTTP calls the available application operations through it. `RestaurantPersistencePort` is the output port: the application asks it to load or save data without depending directly on database technology.

An **adapter** connects a port to a technology. `RestaurantApi` adapts HTTP requests to the input port. `JpaRestaurantAdapter` adapts persistence calls to JPA/Hibernate and PostgreSQL. The adapter uses a Spring Data repository for branch identity/lifecycle operations and JPA native queries for existing joined views, inserts, and report aggregates. Flyway remains responsible for schema changes; Hibernate runs with `ddl-auto: validate`.

## 6. SymmetricDS

**SymmetricDS is not executed as part of the current runnable project.** The app currently connects to one development PostgreSQL database. Real SymmetricDS replication needs separate Central and Branch database nodes, SymmetricDS engine deployments, node identities, channels, triggers, and routing configuration. This is an external infrastructure integration and is documented in [`docs/symmetricds.md`](docs/symmetricds.md).

The application contains no fake sync service, no endpoint that pretends to replicate, and no application-level sync cursor. The automated tests do not execute SymmetricDS replication because this project does not provide a real multi-node database topology.

## 7. Intended SymmetricDS topology

```text
                  CENTRAL
               PostgreSQL DB
                    |
                SymmetricDS
               /           \
              v             v
       TRIVANDRUM DB      KOCHI DB
        PostgreSQL       PostgreSQL
        SymmetricDS      SymmetricDS
```

This is intended deployment behavior, not current runtime behavior. Central-to-Branch routing would send configuration only to branches explicitly enabled for its master. Branch orders, order items, and payments would flow Branch-to-Central for reporting. Branch transactions would not route directly to another branch.

### Alternative: simple payment sync API (proposal)

we can synchronize payments with ordinary HTTP instead of SymmetricDS, each side would have its own PostgreSQL database. A branch-side sync action would read payments that Central has not accepted yet and send them in a batch to a Central API:

```text
TRIVANDRUM RESTAURANT                         CENTRAL HUB
┌──────────────────────────┐                 ┌──────────────────────────┐
│ Branch Spring Boot API   │                 │ Central Spring Boot API  │
│                          │                 │                          │
│ Branch PostgreSQL        │                 │ Central PostgreSQL       │
│ orders + payments        │                 │ imported orders/payments │
└────────────┬─────────────┘                 └────────────▲─────────────┘
             │                                            │
             │ Read unsynced branch payments              │ Validate branch,
             │                                            │ save once, reply
             └── POST /api/central/branches/{branchId}/payments/sync ──┘
```

Example request body (illustrative; this endpoint is **not implemented**):

```json
{
  "payments": [
    {
      "branchPaymentId": "<payment-uuid-created-at-branch>",
      "branchOrderId": "<order-uuid-created-at-branch>",
      "amount": 440.00,
      "method": "UPI",
      "status": "COMPLETED",
      "paidAt": "2026-10-14T10:30:00Z"
    }
  ]
}
```

The branch should mark a payment as synchronized only after Central acknowledges the batch. Central should make retries safe with a unique key such as `(branch_id, branch_payment_id)`, so sending the same payment again does not create a duplicate. Since the current `payment` table refers to an order, the order must be imported before its payments (or Central would need a separate imported-transaction table). This HTTP approach is a simple request/response sync design; authentication, retries, and the endpoint itself still need implementation. The runnable project continues to use one shared database, as described below.

## 8. Intended example

Central creates Pizza → Medium → Pepperoni and explicitly enables its master for Trivandrum only. In a SymmetricDS deployment, Central would route those configuration rows to Trivandrum; Kochi would not receive them unless enabled there too. Trivandrum would later write an order, and SymmetricDS would route that transaction back to Central for reporting. This describes intended deployment behavior; none of those database-to-database transfers run in this sample.

## 9. Current limitations

- SymmetricDS is documented but not runnable here; real replication needs separate PostgreSQL nodes and SymmetricDS deployment/configuration.
- Automated tests verify business/API behavior against an in-memory H2 database; they do not emulate or claim real replication.
- The persistence adapter currently uses Spring Data JPA for branch persistence and JPA/Hibernate native SQL for joined projections and remaining table operations. It is a transitional JPA adapter rather than a separate Spring Data repository for every table.
- Authentication and a real payment gateway are out of scope.

## 10. Run locally

Requirements: Java 21, Maven, Docker Compose.

```powershell
docker compose up -d
mvn spring-boot:run
```

The API listens at `http://localhost:8080`. Local defaults are database `restaurant`, username `restaurant`, password `restaurant`; override them with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. Flyway applies migrations on startup. Schema generation is disabled (`spring.jpa.hibernate.ddl-auto=validate`).

Run tests:

```powershell
mvn test
```

Tests cover unique branch codes, branch-specific master enablement/disablement, branch and customer-specific pricing, order price snapshots, order branch isolation, and payment ownership. No test claims SymmetricDS replication.

## Example API flow

```http
POST /api/central/branches
Content-Type: application/json

{"code":"TVM","name":"Paradise Restaurant Trivandrum","city":"Trivandrum"}
```

Create another branch with code `KOC`, create a master, and enable it only for Trivandrum:

```http
POST /api/central/masters
{"code":"PIZZA","name":"Pizza","type":"MENU"}

POST /api/central/branches/{tvmId}/masters/{pizzaMasterId}/enable
GET  /api/central/branches/{tvmId}/masters
GET  /api/central/branches/{kocId}/masters
```

The enabled master appears only for Trivandrum. For the frontend's all-at-once menu builder, Central can create the master, menu structure, explicit branch assignments, and independent customer-type prices in one transaction:

```http
POST /api/central/master-menus
Content-Type: application/json

{
  "code": "PIZZA",
  "masterName": "Pizza",
  "menuName": "Main Menu",
  "categoryName": "Pizza",
  "itemName": "Pizza",
  "variantName": "Medium Pepperoni",
  "size": "Medium",
  "flavour": "Pepperoni",
  "branches": [
    { "branchId": "<tvm-uuid>", "guestPrice": 220.00, "staffPrice": 180.00 },
    { "branchId": "<koc-uuid>", "guestPrice": 190.00, "staffPrice": 150.00 }
  ]
}
```

Only the listed branches receive this menu. Each listed branch receives its own Guest and Staff prices. The lower-level central menu and branch price endpoints remain available too. Central can update a saved price with `PUT /api/central/branches/{branchId}/prices/{priceId}`; branch reads of `/api/branches/{branchId}/menu` and `/api/branches/{branchId}/prices` remain scoped to that branch's enabled masters.

Set an individual price with `POST /api/central/branches/{branchId}/prices`:

```json
{"menuVariantId":"<variant-uuid>","customerType":"GUEST","amount":220.00,"currency":"INR"}
```

Create an order at `POST /api/branches/{branchId}/orders`:

```json
{"customerType":"GUEST","items":[{"menuVariantId":"<variant-uuid>","quantity":2}]}
```

The order stores the current branch price. Record payment at `POST /api/branches/{branchId}/payments`. Branch reports are available from `/api/central/reports/branches/{branchId}` and `/api/central/reports/branches`.

### How Central gets branch reports in this demo

The current app uses **one shared PostgreSQL database**. When Central calls `GET /api/central/reports/branches/{branchId}`, the API calculates the requested branch's totals from the orders and payments already stored in that database. `GET /api/central/reports/branches` calculates the same kind of summary for each branch, keeping each result labeled with its branch identity. The API scopes each calculation by `branchId` where appropriate; it does not contact a separate branch server or copy data from another database.

Likewise, `GET /api/central/payments` returns the shared-database payment ledger with each record's branch identity. These are Central reads over the shared local database, not branch-to-central replication. If separate databases are introduced later, data would need to be transferred to Central before Central's own database could report on it.
