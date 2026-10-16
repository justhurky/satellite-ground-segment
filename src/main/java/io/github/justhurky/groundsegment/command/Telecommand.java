package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.common.error.IllegalStateTransitionException;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telecommand",
        uniqueConstraints = @UniqueConstraint(name = "uq_telecommand_nonce",
                columnNames = {"spacecraft_id", "nonce"}))
public class Telecommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spacecraft_id", nullable = false)
    private Spacecraft spacecraft;

    @Column(nullable = false, length = 64)
    private String opcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommandCriticality criticality;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommandState state;

    @Column(name = "canonical_parameters", nullable = false, length = 1024)
    private String canonicalParameters;

    @Column(nullable = false, length = 64)
    private String nonce;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "execute_after")
    private Instant executeAfter;

    @Column(nullable = false)
    private byte[] signature;

    @Column(name = "payload_digest", nullable = false, length = 64)
    private String payloadDigest;

    @Column(name = "payload_bytes", nullable = false)
    private int payloadBytes;

    @Column(name = "requested_by", nullable = false, length = 64)
    private String requestedBy;

    @Column(name = "contact_window_id")
    private UUID contactWindowId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "uplinked_at")
    private Instant uplinkedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "outcome_detail", length = 256)
    private String outcomeDetail;

    @Version
    @Column(nullable = false)
    private long version;

    protected Telecommand() {
    }

    public Telecommand(Spacecraft spacecraft, String opcode, CommandCriticality criticality,
                       String canonicalParameters, String nonce, Instant requestedAt, Instant submittedAt,
                       Instant executeAfter, byte[] signature, String payloadDigest, int payloadBytes,
                       String requestedBy) {
        this.spacecraft = spacecraft;
        this.opcode = opcode;
        this.criticality = criticality;
        this.priority = criticality.priority();
        this.state = CommandState.RECEIVED;
        this.canonicalParameters = canonicalParameters;
        this.nonce = nonce;
        this.requestedAt = requestedAt;
        this.submittedAt = submittedAt;
        this.executeAfter = executeAfter;
        this.signature = signature.clone();
        this.payloadDigest = payloadDigest;
        this.payloadBytes = payloadBytes;
        this.requestedBy = requestedBy;
    }

    public void markValidated() {
        transitionTo(CommandState.VALIDATED);
    }

    public void reject(String detail) {
        transitionTo(CommandState.REJECTED);
        this.outcomeDetail = detail;
    }

    public void schedule(UUID windowId, Instant scheduledAt) {
        transitionTo(CommandState.SCHEDULED);
        this.contactWindowId = windowId;
        this.scheduledAt = scheduledAt;
    }

    public void markUplinked(Instant uplinkedAt) {
        transitionTo(CommandState.UPLINKED);
        this.uplinkedAt = uplinkedAt;
    }

    public void markAcknowledged(int resultCode, Instant completedAt) {
        transitionTo(CommandState.ACKNOWLEDGED);
        this.resultCode = resultCode;
        this.completedAt = completedAt;
    }

    public void fail(String detail, Instant completedAt) {
        transitionTo(CommandState.FAILED);
        this.outcomeDetail = detail;
        this.completedAt = completedAt;
    }

    public void fail(int resultCode, String detail, Instant completedAt) {
        transitionTo(CommandState.FAILED);
        this.resultCode = resultCode;
        this.outcomeDetail = detail;
        this.completedAt = completedAt;
    }

    public void cancel(String reason, Instant completedAt) {
        transitionTo(CommandState.CANCELLED);
        this.outcomeDetail = reason;
        this.completedAt = completedAt;
    }

    private void transitionTo(CommandState target) {
        if (!state.canTransitionTo(target)) {
            throw new IllegalStateTransitionException("Command " + id + " cannot move from " + state
                    + " to " + target + ", allowed targets are " + state.allowedTargets());
        }
        this.state = target;
    }

    public UUID getId() {
        return id;
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public String getOpcode() {
        return opcode;
    }

    public CommandCriticality getCriticality() {
        return criticality;
    }

    public int getPriority() {
        return priority;
    }

    public CommandState getState() {
        return state;
    }

    public String getCanonicalParameters() {
        return canonicalParameters;
    }

    public String getNonce() {
        return nonce;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getExecuteAfter() {
        return executeAfter;
    }

    public byte[] getSignature() {
        return signature.clone();
    }

    public String getPayloadDigest() {
        return payloadDigest;
    }

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public UUID getContactWindowId() {
        return contactWindowId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getUplinkedAt() {
        return uplinkedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public String getOutcomeDetail() {
        return outcomeDetail;
    }
}
