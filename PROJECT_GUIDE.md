# Project Guide

## What the application models

One Spring Boot modular monolith contains Central Hub operations and branch operations. The Central Hub manages branches and shared menu configuration. `BranchMasterAssignment` controls which branch can access which centrally managed menu. Branch-specific prices, orders, and payments remain scoped to the branch. The database is shared in this local sample; that is not a substitute for separate branch databases in a future replicated deployment.

## Code map

```text
src/main/java/com/restaurant/ddd/
  RestaurantApplication.java                  Spring Boot entry point
  presentation/
    RestaurantApi.java                         HTTP inbound adapter
    ApiErrors.java                              HTTP error mapping
  application/
    command/RestaurantCommands.java             request/input records and validation
    port/in/RestaurantUseCases.java              input port called by the HTTP adapter
    port/out/RestaurantPersistencePort.java      output port needed by use cases
    service/RestaurantService.java               application use-case coordinator
  domain/
    branch/                                     Branch and explicit master assignment
    order/                                      Order aggregate and item snapshots
    payment/                                    Payment enums
    pricing/                                    Branch-specific price rules
    shared/                                     Shared domain types such as customer type
  adapter/out/persistence/
    JpaRestaurantAdapter.java                    JPA/Hibernate outbound adapter
    entity/BranchJpaEntity.java                  persistence mapping; not a domain class
    repository/SpringDataBranchRepository.java   Spring Data repository for branches
  resources/
    application.yml                              local DB/server defaults and Hibernate validation
    db/migration/                                Flyway schema history

docs/symmetricds.md                              intended replication topology and routing
src/test/java/com/restaurant/ddd/                HTTP integration tests against H2
pom.xml                                           Maven, Java 21, Spring Boot dependencies
Docker-compose.yml                               PostgreSQL only, with named volume
README.md                                         project overview and run/API guide
target/                                           generated Maven output; can be recreated
```

## Request flow

```text
HTTP → RestaurantApi → RestaurantUseCases → RestaurantService
                                      RestaurantService → RestaurantPersistencePort
                                                       → JpaRestaurantAdapter → PostgreSQL
```

A port is an interface at a boundary; an adapter implements that boundary using a technology. `RestaurantUseCases` is the inbound port. `RestaurantPersistencePort` is the output port. This keeps controllers out of business coordination and keeps JDBC/JPA details out of the domain. The persistence adapter uses Spring Data JPA for branch identity/lifecycle operations and JPA native queries for joined result views, inserts, and reports. The current project has one explicit Spring Data repository and a transitional shared persistence port rather than a repository interface for every table.

## Key relationships and rules

- A branch has a unique normalized code.
- A central master is visible to a branch only through an enabled `BranchMasterAssignment` for that branch.
- Menus use menu → category → item → variant.
- Branch price uniqueness is `(branch_id, menu_variant_id, customer_type)`.
- Each order item stores its purchase-time `unit_price`; later price updates do not rewrite orders.
- Payment has both branch and order ownership; the application checks that the order belongs to the requested branch.
- Branch menu, price, order, payment, and report queries carry the branch identity.

## Database and migrations

PostgreSQL is the local runtime database. `docker compose up -d` starts only PostgreSQL. Flyway applies `V1__initial_schema.sql` and later migration files. Hibernate is configured with `ddl-auto: validate`, so it checks mapped entities against the migration-owned schema and does not generate tables. Test configuration uses in-memory H2 in PostgreSQL mode.

## SymmetricDS

The app does not run or imitate SymmetricDS. The simulated `/sync` endpoint and app-owned sync cursor have been removed. A real setup would have separate Central, Trivandrum, and Kochi PostgreSQL nodes, each configured as a SymmetricDS node. Central-to-branch configuration routing would use explicit branch/master assignments; orders, order items, and payments would route back to Central for reports. `GET /api/central/payments` reads payment records from this app's shared database and does not replicate data. Details and limitations are in `docs/symmetricds.md`. Automated tests do not execute replication.

## Run and tests

```powershell
docker compose up -d
mvn spring-boot:run
mvn test
```

Check the toolchain with `mvn -version`, `Get-Command mvn`, `where.exe mvn`, and `java -version`.

## Current boundaries

This remains a learning/interview model, not a production deployment. SymmetricDS topology is documented but absent. Persistence is in a transitional stage: Branch uses a Spring Data repository and the JPA adapter uses native JPA queries for the other table operations and report projections. Expanding the model into per-aggregate Spring Data repositories is a possible next refactor; it should be done when it clarifies the code rather than just multiplying interfaces. H2 tests provide fast business checks but do not replace verification against PostgreSQL.
