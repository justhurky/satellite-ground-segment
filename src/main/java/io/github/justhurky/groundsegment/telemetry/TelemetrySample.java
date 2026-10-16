package io.github.justhurky.groundsegment.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "telemetry_sample")
public class TelemetrySample {

    public static final String UNMAPPED_NAME_PREFIX = "SLOT_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "frame_id", nullable = false)
    private TelemetryFrame frame;

    @Column(name = "parameter_slot", nullable = false)
    private int parameterSlot;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "observed_value", nullable = false)
    private double observedValue;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(nullable = false)
    private boolean mapped;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected TelemetrySample() {
    }

    public TelemetrySample(TelemetryFrame frame, int parameterSlot, String parameterName, double observedValue,
                           String unit, boolean mapped, Instant generatedAt) {
        this.frame = frame;
        this.parameterSlot = parameterSlot;
        this.parameterName = parameterName;
        this.observedValue = observedValue;
        this.unit = unit;
        this.mapped = mapped;
        this.generatedAt = generatedAt;
    }

    public static String unmappedName(int parameterSlot) {
        return UNMAPPED_NAME_PREFIX + parameterSlot;
    }

    public Long getId() {
        return id;
    }

    public TelemetryFrame getFrame() {
        return frame;
    }

    public int getParameterSlot() {
        return parameterSlot;
    }

    public String getParameterName() {
        return parameterName;
    }

    public double getObservedValue() {
        return observedValue;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isMapped() {
        return mapped;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
