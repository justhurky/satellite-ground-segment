package io.github.justhurky.groundsegment.audit.dto;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditEvent;
import java.time.Instant;

public record AuditEventResponse(
        long sequence,
        Instant occurredAt,
        String actor,
        AuditAction action,
        String resourceType,
        String resourceId,
        String details,
        String previousHash,
        String hash) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getEventSequence(),
                event.getOccurredAt(),
                event.getActor(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getDetails(),
                event.getPreviousHash(),
                event.getHash());
    }
}
