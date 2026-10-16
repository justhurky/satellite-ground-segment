package io.github.justhurky.groundsegment.groundstation;

import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_window")
public class ContactWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spacecraft_id", nullable = false)
    private Spacecraft spacecraft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ground_station_id", nullable = false)
    private GroundStation groundStation;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "uplink_budget_bytes", nullable = false)
    private int uplinkBudgetBytes;

    @Column(name = "uplinked_bytes", nullable = false)
    private int uplinkedBytes;

    @Version
    @Column(nullable = false)
    private long version;

    protected ContactWindow() {
    }

    public ContactWindow(Spacecraft spacecraft, GroundStation groundStation, Instant startsAt, Instant endsAt,
                         int uplinkBudgetBytes) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("A contact window must end after it starts");
        }
        this.spacecraft = spacecraft;
        this.groundStation = groundStation;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.uplinkBudgetBytes = uplinkBudgetBytes;
        this.uplinkedBytes = 0;
    }

    public boolean isOpenAt(Instant instant) {
        return !instant.isBefore(startsAt) && instant.isBefore(endsAt);
    }

    public int remainingBudgetBytes() {
        return uplinkBudgetBytes - uplinkedBytes;
    }

    public boolean reserve(int bytes) {
        if (bytes > remainingBudgetBytes()) {
            return false;
        }
        this.uplinkedBytes += bytes;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getUplinkBudgetBytes() {
        return uplinkBudgetBytes;
    }

    public int getUplinkedBytes() {
        return uplinkedBytes;
    }
}
