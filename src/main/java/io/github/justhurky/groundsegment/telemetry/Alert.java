package io.github.justhurky.groundsegment.telemetry;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spacecraft_id", nullable = false)
    private Spacecraft spacecraft;

    @Column(name = "frame_id", nullable = false)
    private long frameId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "observed_value", nullable = false)
    private double observedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_kind", nullable = false, length = 16)
    private LimitBreach.LimitKind limitKind;

    @Column(name = "limit_value", nullable = false)
    private double limitValue;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    protected Alert() {
    }

    public Alert(Spacecraft spacecraft, long frameId, String parameterName, double observedValue,
                 LimitBreach breach, Instant raisedAt) {
        this.spacecraft = spacecraft;
        this.frameId = frameId;
        this.parameterName = parameterName;
        this.observedValue = observedValue;
        this.severity = breach.severity();
        this.limitKind = breach.limitKind();
        this.limitValue = breach.threshold();
        this.raisedAt = raisedAt;
        this.acknowledged = false;
    }

    public void acknowledge(String actor, Instant acknowledgedAt) {
        if (acknowledged) {
            throw new IllegalStateTransitionException("Alert " + id + " was already acknowledged by "
                    + acknowledgedBy);
        }
        this.acknowledged = true;
        this.acknowledgedBy = actor;
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUID getId() {
        return id;
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public long getFrameId() {
        return frameId;
    }

    public String getParameterName() {
        return parameterName;
    }

    public double getObservedValue() {
        return observedValue;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public LimitBreach.LimitKind getLimitKind() {
        return limitKind;
    }

    public double getLimitValue() {
        return limitValue;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
