# 5. A hash-chained audit trail

Status: accepted

## Context

A mission needs to answer, after an anomaly, who commanded what and which measurements the decision
was based on. An ordinary audit table answers that only as long as nobody edits it. Anyone with
database access can change a row or delete one, and nothing in the table reveals it.

## Decision

Every audit entry stores the hash of the previous entry, and its own hash covers both that link and
its canonicalised content:

```
SHA-256(previousHash || 0x1F || sequence|timestamp|actor|action|resourceType|resourceId|details)
```

The `0x1F` separator keeps field boundaries unambiguous, so no pair of different entries can hash
the same way. `GET /api/v1/audit/integrity` re-walks the chain, recomputes every hash and reports
the first sequence number where continuity, linkage or the hash breaks.

Appending is serialised and flushed inside its transaction so the chain cannot fork. Rejected
requests record their audit entry in a separate transaction, so the evidence survives the rollback
of the request that failed.

## Consequences

Any edit or deletion becomes detectable, and the detection is an endpoint an auditor can call rather
than a manual investigation. Two constraints follow: timestamps are truncated to milliseconds,
because the chain covers the timestamp and it has to survive a database round trip byte for byte,
and the audit writer is a serialisation point, which is acceptable at this scale but would need
partitioning per stream under heavy load.
