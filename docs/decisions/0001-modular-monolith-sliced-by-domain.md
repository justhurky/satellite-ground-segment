# 1. A modular monolith sliced by domain

Status: accepted

## Context

The ground segment has several concerns that evolve at different speeds: telemetry ingestion,
telecommanding, the spacecraft registry, scheduling and auditing. A classic `controller / service /
repository` package layout puts unrelated code next to each other and hides which parts actually
depend on which. Splitting into separate services at this size would buy nothing but operational
cost.

## Decision

One deployable application, with top-level packages per domain slice, each owning its entities,
repositories, services and endpoints. Cross-slice calls go through services, or through Spring
application events where a direct dependency would create a cycle: telemetry publishes
`CommandAcknowledgementReceived` and the command slice listens.

Dependency rules are enforced by ArchUnit rather than by convention: the slices must stay free of
cycles, controllers must not be called from inside the application, and controllers must not reach
repositories.

## Consequences

The boundaries that would matter if a slice ever became its own service are already visible and
tested. Two concessions were needed to keep the graph acyclic: the actor lookup used by the audit
trail sits behind a `common.security.ActorProvider` interface so that `audit` does not depend on
`identity`, and the security configuration lives in `identity` rather than `config` so that `config`
depends on nothing internal.
