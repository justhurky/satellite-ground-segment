package io.github.justhurky.groundsegment.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sequence_state",
        uniqueConstraints = @UniqueConstraint(name = "uq_sequence_state_stream",
                columnNames = {"spacecraft_id", "apid"}))
public class SequenceState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spacecraft_id", nullable = false)
    private UUID spacecraftId;

    @Column(nullable = false)
    private int apid;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;

    @Column(name = "seen_mask", nullable = false)
    private long seenMask;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SequenceState() {
    }

    public SequenceState(UUID spacecraftId, int apid, ReplayWindow window, Instant updatedAt) {
        this.spacecraftId = spacecraftId;
        this.apid = apid;
        this.lastSequence = window.lastSequence();
        this.seenMask = window.seenMask();
        this.updatedAt = updatedAt;
    }

    public ReplayWindow window() {
        return new ReplayWindow(lastSequence, seenMask);
    }

    public void advanceTo(ReplayWindow window, Instant updatedAt) {
        this.lastSequence = window.lastSequence();
        this.seenMask = window.seenMask();
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getSpacecraftId() {
        return spacecraftId;
    }

    public int getApid() {
        return apid;
    }

    public int getLastSequence() {
        return lastSequence;
    }

    public long getSeenMask() {
        return seenMask;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
