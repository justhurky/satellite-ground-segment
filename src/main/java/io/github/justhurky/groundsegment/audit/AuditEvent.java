package io.github.justhurky.groundsegment.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    public static final String FIELD_SEPARATOR = "|";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_sequence", nullable = false, unique = true)
    private long eventSequence;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 64)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private AuditAction action;

    @Column(name = "resource_type", nullable = false, length = 48)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(nullable = false, length = 2048)
    private String details;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, length = 64)
    private String hash;

    protected AuditEvent() {
    }

    public AuditEvent(long eventSequence, Instant occurredAt, String actor, AuditAction action,
                      String resourceType, String resourceId, Map<String, String> details, String previousHash) {
        this.eventSequence = eventSequence;
        this.occurredAt = occurredAt.truncatedTo(ChronoUnit.MILLIS);
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = canonicalDetails(details);
        this.previousHash = previousHash;
    }

    public static String canonicalDetails(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        return new TreeMap<>(details).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Objects.toString(entry.getValue(), ""))
                .collect(Collectors.joining(";"));
    }

    public String canonicalRecord() {
        return String.join(FIELD_SEPARATOR,
                Long.toString(eventSequence),
                occurredAt.toString(),
                actor,
                action.name(),
                resourceType,
                Objects.toString(resourceId, ""),
                details);
    }

    public void sealWith(String computedHash) {
        this.hash = computedHash;
    }

    public Long getId() {
        return id;
    }

    public long getEventSequence() {
        return eventSequence;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDetails() {
        return details;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }
}
