# 4. A persisted sliding window for telemetry replay protection

Status: accepted

## Context

CCSDS sequence counters are 14 bits, so they wrap every 16384 packets. Frames also arrive out of
order when several ground stations downlink the same pass. Two naive options both fail:

- "the counter must be greater than the last one" drops legitimate out-of-order frames and breaks
  completely at wraparound
- storing every counter ever seen grows without bound and still needs wraparound handling

Keeping the state in memory would also mean a restart silently reopens the replay window.

## Decision

A sliding window of 64 slots per spacecraft and APID, stored as the highest sequence counter seen
plus a 64-bit mask of the preceding slots, in the same spirit as IPsec anti-replay. Admission is
decided on the modular distance in the 14-bit space: the current head is a duplicate, a forward
distance shifts the mask and becomes the new head, a backward distance inside the window is accepted
once, and anything older than the window is refused as unverifiable.

The decision logic is an immutable record, `ReplayWindow`, with a pure `accept` method. Persistence
lives in `sequence_state`, read under a pessimistic write lock so two concurrent ingests cannot both
admit the same counter.

## Consequences

The guard is constant in size, correct across wraparound, survives restarts, and is tested as pure
logic rather than through the database. The cost is one locked row read and update per ingested
frame, and the window is bounded at 64 slots because the mask is a `long`; a wider window would need
a different representation.
