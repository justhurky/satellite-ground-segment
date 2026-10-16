# API

Base path `/api/v1`. Every endpoint except login, health, info and the documentation requires
`Authorization: Bearer <token>`. The generated OpenAPI 3 document is served at `/v3/api-docs` and
Swagger UI at `/swagger-ui.html`.

## Endpoints

| Method | Path                                                              | Roles                                        |
|--------|-------------------------------------------------------------------|----------------------------------------------|
| POST   | `/auth/login`                                                     | public                                       |
| GET    | `/auth/me`                                                        | any authenticated                            |
| POST   | `/users`                                                          | `ADMIN`                                      |
| GET    | `/users`                                                          | `ADMIN`                                      |
| POST   | `/spacecraft`                                                     | `ADMIN`                                      |
| GET    | `/spacecraft`                                                     | any authenticated                            |
| GET    | `/spacecraft/{id}`                                                | any authenticated                            |
| PUT    | `/spacecraft/{id}/mission-phase`                                  | `FLIGHT_DIRECTOR`, `ADMIN`                   |
| POST   | `/spacecraft/{id}/parameters`                                     | `ADMIN`, `FLIGHT_DIRECTOR`                   |
| GET    | `/spacecraft/{id}/parameters`                                     | any authenticated                            |
| POST   | `/ground-stations`                                                | `ADMIN`                                      |
| GET    | `/ground-stations`                                                | any authenticated                            |
| POST   | `/spacecraft/{id}/contact-windows`                                | `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN`       |
| GET    | `/spacecraft/{id}/contact-windows`                                | any authenticated                            |
| POST   | `/telemetry/frames`                                               | `GROUND_STATION`, `ADMIN`                    |
| GET    | `/spacecraft/{id}/telemetry/frames`                               | `ANALYST`, `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN` |
| GET    | `/telemetry/frames/{frameId}/samples`                             | `ANALYST`, `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN` |
| GET    | `/spacecraft/{id}/telemetry/parameters/{name}/series`             | `ANALYST`, `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN` |
| GET    | `/spacecraft/{id}/alerts`                                         | `ANALYST`, `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN` |
| POST   | `/alerts/{id}/acknowledge`                                        | `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN`       |
| POST   | `/spacecraft/{id}/commands`                                       | `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN`       |
| GET    | `/spacecraft/{id}/commands`                                       | any authenticated                            |
| GET    | `/commands/{id}`                                                  | any authenticated                            |
| GET    | `/commands/catalogue`                                             | any authenticated                            |
| POST   | `/commands/{id}/cancel`                                           | `OPERATOR`, `FLIGHT_DIRECTOR`, `ADMIN`       |
| GET    | `/audit`                                                          | `AUDITOR`, `ADMIN`                           |
| GET    | `/audit/integrity`                                                | `AUDITOR`, `ADMIN`                           |

Collection endpoints take `page` and `size`; telemetry and command queries additionally take
`apid`, `from`, `to`, `state`, `severity` and `acknowledged` where meaningful.

## Error codes

Failures are RFC 7807 problem documents with a stable `errorCode` property.

| `errorCode`                   | Status | Meaning                                                   |
|-------------------------------|--------|-----------------------------------------------------------|
| `VALIDATION_FAILED`           | 400    | Payload or query parameters failed validation             |
| `FRAME_MALFORMED`             | 400    | The frame violates the CCSDS structure or the size limit   |
| `AUTHENTICATION_FAILED`       | 401    | Missing, expired or invalid bearer token                  |
| `FRAME_AUTHENTICATION_FAILED` | 401    | The frame authentication tag does not match               |
| `ACCESS_DENIED`               | 403    | The caller lacks the required role                        |
| `RESOURCE_NOT_FOUND`          | 404    | No such spacecraft, station, frame, alert or command       |
| `DUPLICATE_RESOURCE`          | 409    | Name, NORAD id, nonce or overlapping window already exists |
| `FRAME_REPLAY_DETECTED`       | 409    | The sequence counter was already seen or is too old        |
| `ILLEGAL_STATE_TRANSITION`    | 409    | The requested lifecycle or mission-phase move is forbidden |
| `COMMAND_SIGNATURE_INVALID`   | 422    | Reserved for signature faults surfaced without a command   |
| `COMMAND_NOT_PERMITTED`       | 422    | Catalogue, criticality or mission-phase rule refused it     |
| `COMMAND_STALE`               | 422    | The command timestamp is outside the accepted clock skew   |
| `KEY_MATERIAL_UNUSABLE`       | 500    | Stored key material cannot be used                         |
| `INTERNAL_ERROR`              | 500    | Unexpected failure                                         |

A command rejected for a bad signature is answered with `422` and the command document itself, in
state `REJECTED`, rather than a problem document, because the command was accepted into the system
and recorded.

## Wire formats

### Telemetry frame

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-----+-+-+-----------+-----+-------------+-------------------+
|ver=0|0|1|   APID    |seq f|  seq count  |    data length    |   primary header, 6 bytes
+-----+-+-+-----------+-----+-------------+-------------------+
|     days since 1958-01-01 |   milliseconds of day ...       |   CDS secondary header, 6 bytes
+---------------------------+---------------------------------+
| slot | IEEE 754 double                                      |   one 9-byte sample, repeated
+------+------------------------------------------------------+
```

`data length` is the number of octets after the primary header minus one, as CCSDS requires.
`ver` must be 0, the type bit must be 0 (telemetry), and the secondary header flag must be 1.
The payload length must be a whole multiple of 9 bytes.

### Command acknowledgement

A frame on the acknowledgement APID (2047 by default) carries exactly 17 payload bytes: the
command UUID as two big-endian 64-bit halves, then a one-byte result code, where 0 means executed.

## Configuration

| Property                                            | Default | Meaning                                  |
|-----------------------------------------------------|---------|------------------------------------------|
| `ground-segment.jwt.secret`                         | none    | HMAC signing secret, at least 32 bytes   |
| `ground-segment.jwt.access-token-ttl`               | `PT30M` | Token lifetime                           |
| `ground-segment.jwt.issuer`                         | set     | Expected and emitted issuer              |
| `ground-segment.crypto.data-encryption-key`         | none    | Base64 AES key, 16 or 32 bytes           |
| `ground-segment.telemetry.replay-window-size`       | `64`    | Replay window slots, at most 64          |
| `ground-segment.telemetry.max-frame-bytes`          | `4096`  | Largest accepted frame                   |
| `ground-segment.telemetry.acknowledgement-apid`     | `2047`  | APID carrying command acknowledgements   |
| `ground-segment.uplink.max-commands-per-pass`       | `20`    | Commands dispatched per contact window   |
| `ground-segment.uplink.max-clock-skew`              | `PT5M`  | Accepted command timestamp skew          |
| `ground-segment.uplink.dispatch-interval`           | `5s`    | Dispatcher fixed delay                   |
| `ground-segment.commands.catalogue.<opcode>`        | eight   | Criticality, phases, role, payload size  |
