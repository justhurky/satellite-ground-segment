# 3. Telecommands are signed over a canonical byte representation

Status: accepted

## Context

Authenticating the operator with a bearer token proves who called the API. It does not prove that
the command body is the one the mission intended, and it does not survive the token being stolen.
A telecommand needs end-to-end integrity that is independent of the transport and of the session.

Signing the serialised JSON would be fragile: key order, whitespace, number formatting and date
formatting all change the bytes without changing the meaning, so a valid command could fail
verification and, worse, two different commands could share a signed representation.

## Decision

Ed25519 signatures, verified against a public key registered per spacecraft, over an explicitly
canonical message:

```
<spacecraftId>\n<OPCODE>\n<CRITICALITY>\n<nonce>\n<requestedAt as epoch millis>\n<k=v;... sorted>
```

The opcode is normalised to upper case with underscores, the timestamp is signed as epoch
milliseconds rather than formatted text, parameters are sorted by key, and both the spacecraft id
and the criticality are inside the signed bytes. Ed25519 comes from the JDK, so no additional
cryptography provider is needed.

Commands that fail verification are persisted as `REJECTED` with the reason, audited, and answered
with `422`.

## Consequences

A command cannot be redirected to another spacecraft, downgraded to a lower criticality to escape
the role check, or replayed after the fact, since the nonce is unique per spacecraft and the
timestamp must fall inside a bounded skew window. The canonical form is now part of the public
contract, which is why it is documented and why `tools/sign-command.jsh` reimplements it
independently as a reference client.
