# 6. The command catalogue lives in configuration

Status: accepted

## Context

Which opcodes exist, how dangerous each one is, in which mission phases it may be sent and which
role may send it are mission facts, not application logic. They change between missions and during a
mission, for example when a phase is restricted after an anomaly. Encoding them in Java means a code
change, a review and a release for what is operationally a parameter change.

## Decision

The catalogue is a validated `@ConfigurationProperties` map under
`ground-segment.commands.catalogue`. Each opcode declares its criticality, the mission phases where
it is allowed, the role required to issue it, and its payload size in bytes. Keys are normalised to
upper case with underscores, so `set-payload-mode` in YAML and `SET_PAYLOAD_MODE` on the wire are the
same opcode, and lookup is case-insensitive.

`CommandAuthorisationService` is the single place that reads it, and it checks, in order: the opcode
exists, the submitted criticality matches the catalogue, the spacecraft still accepts commanding, the
mission phase allows the opcode, the caller holds the required role, and the timestamp is within the
accepted clock skew.

## Consequences

Adding an opcode or restricting one to another phase is a configuration change that fails fast at
start-up if it is invalid. The rules are also directly testable without the web or persistence
layers, which is how they are tested. The trade-off is that the catalogue is now part of the
deployment surface, so a misconfiguration is an operational risk, which is why every field is
validated and why the payload size is bounded.
