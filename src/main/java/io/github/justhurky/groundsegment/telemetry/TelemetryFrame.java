package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.groundstation.GroundStation;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "telemetry_frame")
public class TelemetryFrame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spacecraft_id", nullable = false)
    private Spacecraft spacecraft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ground_station_id", nullable = false)
    private GroundStation groundStation;

    @Column(nullable = false)
    private int apid;

    @Column(name = "sequence_count", nullable = false)
    private int sequenceCount;

    @Column(name = "sequence_flags", nullable = false)
    private int sequenceFlags;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "payload_length", nullable = false)
    private int payloadLength;

    @Column(name = "payload_sealed", nullable = false)
    private byte[] payloadSealed;

    @OneToMany(mappedBy = "frame", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TelemetrySample> samples = new ArrayList<>();

    protected TelemetryFrame() {
    }

    public TelemetryFrame(Spacecraft spacecraft, GroundStation groundStation, DecodedFrame decoded,
                          byte[] payloadSealed, Instant receivedAt) {
        this.spacecraft = spacecraft;
        this.groundStation = groundStation;
        this.apid = decoded.apid();
        this.sequenceCount = decoded.sequenceCount();
        this.sequenceFlags = decoded.sequenceFlags();
        this.generatedAt = decoded.generatedAt();
        this.receivedAt = receivedAt;
        this.payloadLength = decoded.payloadLength();
        this.payloadSealed = payloadSealed.clone();
    }

    public void addSample(TelemetrySample sample) {
        samples.add(sample);
    }

    public String sealingContext() {
        return "telemetry-payload:" + spacecraft.getId() + ":" + apid;
    }

    public static String sealingContextFor(Spacecraft spacecraft, int apid) {
        return "telemetry-payload:" + spacecraft.getId() + ":" + apid;
    }

    public Long getId() {
        return id;
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public int getApid() {
        return apid;
    }

    public int getSequenceCount() {
        return sequenceCount;
    }

    public int getSequenceFlags() {
        return sequenceFlags;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public byte[] getPayloadSealed() {
        return payloadSealed.clone();
    }

    public List<TelemetrySample> getSamples() {
        return List.copyOf(samples);
    }
}
