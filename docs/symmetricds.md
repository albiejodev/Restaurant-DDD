# SymmetricDS integration design

## What it is

SymmetricDS is database replication software. It observes database changes at configured nodes and transports selected rows to other configured database nodes. It is infrastructure alongside the Spring Boot application; it is not a business service called by order or menu use cases.

## Why it does not run in this project

This sample currently has one development PostgreSQL database shared by the app. Replication cannot be demonstrated meaningfully without separate Central and Branch database nodes and SymmetricDS engines/configuration. The project therefore includes no fake `SyncService`, no simulated sync endpoint, and no app-managed sync cursor. Automated tests do not execute replication.

## Required topology

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

Each physical database has a SymmetricDS node/engine with node identity, registration/configuration, database triggers, channels, and a network path to the other nodes. Exact engine deployment and settings belong to the eventual environment and are intentionally absent from the runnable Spring Boot sample.

## Central to Branch configuration routing

A Central master must be explicitly assigned/enabled for a branch. Routing configuration would use the branch-master assignment to select the destination node. If Pizza is enabled for Trivandrum only, Pizza menu/configuration rows route Central → Trivandrum and not to Kochi. Enabling Pizza for Kochi later would add that branch as a destination. The current app enforces assignment when serving branch menus and prices; SymmetricDS routing itself is not configured or running here.

## Branch to Central transaction routing

Orders, order items, and payments created in a branch database would replicate Branch → Central so Central can aggregate reports. Routing should not send Trivandrum transactions directly to Kochi or Kochi transactions directly to Trivandrum; Central is the reporting aggregation point.

## Isolation

Each branch-owned row carries its branch identity (directly or through its order/assignment parent). Branch API queries include `branchId`. In a multi-node deployment, row-level routing should additionally deliver configuration only to assigned branch nodes. SymmetricDS transport rules complement the application's branch-scoped reads; they do not replace those business checks.

## Intended flow (not current runtime behavior)

1. Central creates Pizza → Medium → Pepperoni.
2. Central enables Pizza for Trivandrum only.
3. SymmetricDS routes that configuration from Central DB to Trivandrum DB; Kochi receives no Pizza rows.
4. Trivandrum creates an order locally.
5. SymmetricDS routes the order, order items, and any payment back to Central for reporting.

The current sample implements the data model and branch rules in one database. It does not execute steps 3 or 5 across database nodes.
