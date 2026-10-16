# Security model

## Threats this service is built against

| Threat                                                      | Control                                                      |
|-------------------------------------------------------------|--------------------------------------------------------------|
| A forged telemetry frame injected into the ground network   | HMAC-SHA256 over the whole frame with a per-spacecraft key    |
| A genuine frame captured and replayed                       | Persisted 64-slot sliding replay window per spacecraft and APID |
| A malformed frame crashing or confusing the decoder          | Strict CCSDS structural validation before any interpretation  |
| Telemetry payloads readable in a database dump               | AES-GCM sealing with context-bound additional data            |
| A forged or altered telecommand                              | Ed25519 signature over a canonical byte representation        |
| A captured command replayed later                            | Per-spacecraft nonce uniqueness plus a bounded clock-skew window |
| A command downgraded to a lower criticality in transit       | The criticality is part of the signed canonical form          |
| An operator issuing a command above their authority          | Per-opcode required role, enforced before signature checking  |
| A command issued in a mission phase that cannot survive it   | Per-opcode mission-phase allowlist in configuration           |
| Silent tampering with the operational history                | Append-only audit log with a SHA-256 hash chain and a verification endpoint |
| A stolen token used indefinitely                            | Short-lived stateless JWTs with issuer and expiry validation   |
| Forged log lines injected through a request path or message  | Control characters replaced and values truncated before they reach the log |
| An oversized payload forcing a large allocation              | The sealer refuses anything above its plaintext limit before allocating    |
| Secrets committed to the repository                          | No production defaults; start-up fails without the required environment variables |

## Frame authentication

Every spacecraft gets a 32-byte HMAC key, generated server-side at registration and returned exactly
once in the registration response. At rest the key is sealed with AES-GCM under the service's data
encryption key, with the additional authenticated data bound to the spacecraft name, so a sealed key
cannot be moved between spacecraft rows.

The ground station computes `HMAC-SHA256(key, frame)` over the complete frame, headers included, and
submits it alongside the frame. Verification uses `MessageDigest.isEqual`, which is constant-time,
and rejects any tag whose length is not 32 bytes before comparing.

## Replay protection

The guard keeps, per spacecraft and APID, the highest sequence counter seen and a 64-bit mask of the
preceding slots. For an incoming counter it computes the modular distance in the 14-bit sequence
space:

- distance zero means the current head, so the frame is a duplicate
- a distance in the forward half shifts the mask and accepts the frame as the new head
- a distance in the backward half is accepted only if it falls inside the window and its bit is not
  yet set
- anything older than the window is refused as unverifiable rather than accepted

The state lives in `sequence_state`, read under a pessimistic write lock, so concurrent ingestion
from several ground stations cannot both admit the same counter. `ReplayWindow` itself is an
immutable record with a pure `accept` method, which is what the unit tests exercise, wraparound
included.

## Command signing

The signed message is a canonical string, newline separated, with no room for ambiguity:

```
<spacecraftId>\n<OPCODE>\n<CRITICALITY>\n<nonce>\n<requestedAt as epoch millis>\n<k1=v1;k2=v2 sorted by key>
```

Details that matter:

- the opcode is normalised to upper case with underscores, so `thruster-burn` and `THRUSTER_BURN`
  produce the same signature
- the timestamp is signed as epoch milliseconds, never as formatted text, so no serialisation
  difference between client and server can invalidate a valid signature or validate an invalid one
- parameters are sorted by key, so map iteration order cannot change the message
- the spacecraft id is included, so a command signed for one spacecraft cannot be redirected to
  another
- the criticality is included, so it cannot be downgraded to escape the role check

`tools/sign-command.jsh` is the reference client-side signer and intentionally reimplements the
canonical form, so the two implementations can be compared.

A command whose signature does not verify is still persisted, in state `REJECTED` with the reason
recorded, and answered with `422`. Forged attempts are evidence and are kept.

## Authorisation

Six roles: `ADMIN`, `FLIGHT_DIRECTOR`, `OPERATOR`, `ANALYST`, `AUDITOR`, `GROUND_STATION`.
The filter chain is stateless, CSRF is off because there are no cookies, and only the login endpoint,
health, info and the API documentation are public. Endpoint authorisation is declared per handler
with `@PreAuthorize`, and the commanding path adds a second, data-dependent decision from the
catalogue.

Authentication and authorisation failures are answered as RFC 7807 problem documents with stable
error codes, and never leak whether an account exists.

## Audit trail

Each entry stores its sequence number, timestamp, actor, action, resource, canonicalised details and
the hash of the previous entry. Its own hash is

```
SHA-256(previousHash || 0x1F || sequence|timestamp|actor|action|resourceType|resourceId|details)
```

The separator byte prevents field-boundary collisions. `GET /api/v1/audit/integrity` walks the chain,
recomputes every hash and reports the first sequence number where continuity, linkage or the hash
itself breaks. Appending is serialised and flushed inside its transaction, so the chain cannot fork
under concurrency.

## Secrets and keys

| Value                                | Source                                                        |
|--------------------------------------|---------------------------------------------------------------|
| JWT signing secret                   | `GROUND_SEGMENT_JWT_SECRET`, no default outside `demo`        |
| Data encryption key (AES-128 or 256) | `GROUND_SEGMENT_DATA_ENCRYPTION_KEY`, no default outside `demo` |
| Spacecraft telemetry HMAC key        | generated at registration, returned once, sealed at rest      |
| Mission command signing key          | never reaches the service; only the Ed25519 public key is stored |

The `demo` profile contains development keys and prints the generated telemetry key to the log so
that the walkthrough is reproducible. Those keys are public by design and must never be used
anywhere else; the profile exists for demonstration only.

## Static analysis

CodeQL runs the extended security query set on every push and once a week. Two findings were real
and are fixed: request paths and exception messages are passed through `LogSanitiser` before they
reach the log, so a newline in a URL cannot forge a log line, and `PayloadSealer` rejects a plaintext
above its limit before allocating the buffer that holds it.

One finding is deliberately not acted on. CodeQL reports the disabled CSRF protection in the security
filter chain. CSRF is an attack on ambient credentials, and this API has none: it is stateless, it
authenticates every request with a bearer token that a browser never attaches on its own, and it
issues no session cookie. Enabling CSRF tokens here would add a ceremony that protects nothing.

## What is deliberately out of scope

This is a portfolio project, not a flight-qualified system. Absent on purpose: key rotation and a
key-management service, mutual TLS between the ground station and the service, rate limiting,
hardware security module support, CCSDS SDLS, two-person authorisation for critical commands, and a
real uplink transport. The simulated uplink channel sits behind the `UplinkChannel` interface so a
real one can replace it without touching the lifecycle logic.
