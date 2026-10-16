# Architecture

## Shape of the system

One Spring Boot application, sliced by domain. Each slice owns its entities, its repositories, its
transactional services and its web endpoints. Nothing reaches into another slice's persistence;
slices talk to each other through services, or through application events where a hard dependency
would create a cycle.

An ArchUnit test (`ArchitectureRulesTest`) enforces the properties that are easy to erode:

- the slices form a directed acyclic graph
- no production class calls a controller
- controllers never touch repositories
- entities never appear in a `dto` package
- every request and response payload is a record
- collaborators are injected through constructors, never into fields
- `javax.crypto` is reachable only from `crypto` and `identity`

## Request paths

### Telemetry ingestion

```
POST /api/v1/telemetry/frames
   │
   ├─ resolve spacecraft and ground station
   ├─ size check against ground-segment.telemetry.max-frame-bytes
   ├─ HMAC-SHA256 tag check with MessageDigest.isEqual                 → 401 on mismatch
   ├─ CCSDS primary + CDS secondary header decode                      → 400 on structural fault
   ├─ replay window admission, pessimistically locked per stream       → 409 on replay
   ├─ AES-GCM seal of the raw payload, AAD = spacecraft id + APID
   ├─ APID = acknowledgement APID ?
   │     ├─ yes → decode acknowledgement, publish application event
   │     └─ no  → decode samples, map to the parameter dictionary, check limits, raise alerts
   └─ audit entry, metrics, response
```

Rejections are audited in their own transaction (`REQUIRES_NEW`) so that the audit entry survives
the rollback of the failed request. Accepted frames are audited in the request transaction, so a
frame and its audit entry commit together or not at all.

### Telecommanding

```
POST /api/v1/spacecraft/{id}/commands
   │
   ├─ nonce uniqueness per spacecraft                                  → 409 on reuse
   ├─ catalogue lookup of the opcode                                   → 422 when unknown
   ├─ criticality must match the catalogue                             → 422 on mismatch
   ├─ mission phase must allow the opcode                              → 422 when not allowed
   ├─ caller must hold the required role                               → 403 otherwise
   ├─ requestedAt within ground-segment.uplink.max-clock-skew          → 422 when stale
   ├─ Ed25519 verification over the canonical form
   │     ├─ fails → persist the command as REJECTED, audit, answer 422
   │     └─ holds → persist as VALIDATED, audit, answer 201
```

The scheduled dispatcher then runs per spacecraft that has validated commands:

```
UplinkDispatcher (fixed delay, ground-segment.uplink.dispatch-interval)
   └─ UplinkPassService.executePass(spacecraftId)          transactional
        ├─ find an open contact window with remaining budget, else stop
        ├─ take up to max-commands-per-pass due commands,
        │  ordered by criticality descending then submission time
        └─ for each: reserve budget → SCHEDULED → transmit → UPLINKED or FAILED
```

A spacecraft acknowledgement arrives later as a telemetry frame on APID 2047 carrying the command
UUID and a result code. Telemetry ingestion publishes `CommandAcknowledgementReceived`, the command
slice listens inside the same transaction, and the command moves to `ACKNOWLEDGED` or `FAILED`.
An acknowledgement for an unknown command, or for one not in `UPLINKED`, is logged and ignored
rather than rejecting an otherwise valid telemetry frame.

## State machines

Both lifecycles are declared as transition tables, and every move goes through a guard that throws
`IllegalStateTransitionException` (HTTP 409) instead of silently overwriting a status field.

```
Command:   RECEIVED ─→ VALIDATED ─→ SCHEDULED ─→ UPLINKED ─→ ACKNOWLEDGED
                │           │            │           └─────→ FAILED
                │           └────────────┴─────────────────→ CANCELLED, FAILED
                └─→ REJECTED

Mission:   COMMISSIONING ─→ NOMINAL ⇄ SAFE_MODE
                   └───────────┴──────────┴─────→ DECOMMISSIONED
```

`DECOMMISSIONED` is terminal and refuses all commanding, which is checked before the opcode's own
phase rule so that the stronger statement wins.

## Persistence

Flyway owns the schema; Hibernate validates against it on every boot (`ddl-auto=validate`), so a
mapping that drifts from a migration fails the build rather than a production start-up. The same
migrations run on H2 in PostgreSQL compatibility mode for fast tests and on real PostgreSQL in the
Testcontainers test.

Decisions worth naming:

- `Instant` columns are `timestamp with time zone` and Hibernate is pinned to `TIMESTAMP_UTC`
- enum columns are strings with database check constraints, so a bad value cannot be written even
  outside the application
- audit timestamps are truncated to milliseconds, because the hash chain covers the timestamp and
  must survive a database round trip unchanged
- `contact_window`, `sequence_state` and `telecommand` carry optimistic-locking versions; the replay
  guard additionally takes a pessimistic write lock on its stream row
- the replay window is stored as a 64-bit mask plus the highest sequence seen, which keeps the guard
  stateless between requests and correct across restarts

## Boundaries and mapping

Controllers are thin: they bind and validate the request, call one service method and return a
record. All entity-to-DTO mapping happens inside the transaction, because `spring.jpa.open-in-view`
is disabled; a lazy association touched in the controller would otherwise fail at runtime. The
services return records for every read path and keep entity returns for internal callers only.

## Configuration

Everything tunable is bound to validated records under the `ground-segment` prefix: JWT settings,
the data-encryption key, telemetry limits, uplink parameters, and the command catalogue. Invalid or
missing values fail fast at start-up. The command catalogue is configuration rather than code, so a
mission can add an opcode, move it to another criticality class or restrict it to another mission
phase without touching Java.

## Observability

Micrometer counters cover accepted and rejected frames by reason, raised alerts by severity, and
commands by state and criticality. Actuator exposes health and Prometheus metrics; `/actuator/**`
beyond health and info requires the `ADMIN` role.
