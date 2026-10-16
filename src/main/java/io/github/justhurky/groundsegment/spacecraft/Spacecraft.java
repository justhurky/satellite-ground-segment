package io.github.justhurky.groundsegment.spacecraft;

import io.github.justhurky.groundsegment.common.error.IllegalStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spacecraft")
public class Spacecraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "norad_id", nullable = false, unique = true)
    private int noradId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_phase", nullable = false, length = 32)
    private MissionPhase missionPhase;

    @Column(name = "telemetry_key_sealed", nullable = false)
    private byte[] telemetryKeySealed;

    @Column(name = "command_public_key", nullable = false)
    private byte[] commandPublicKey;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected Spacecraft() {
    }

    public Spacecraft(String name, int noradId, byte[] telemetryKeySealed, byte[] commandPublicKey,
                      Instant registeredAt) {
        this.name = name;
        this.noradId = noradId;
        this.missionPhase = MissionPhase.COMMISSIONING;
        this.telemetryKeySealed = telemetryKeySealed.clone();
        this.commandPublicKey = commandPublicKey.clone();
        this.registeredAt = registeredAt;
    }

    public void transitionTo(MissionPhase target) {
        if (missionPhase == target) {
            return;
        }
        if (!missionPhase.canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    "Mission phase " + missionPhase + " cannot transition to " + target);
        }
        this.missionPhase = target;
    }

    public static String sealingContextFor(String spacecraftName) {
        return "spacecraft-telemetry-key:" + spacecraftName;
    }

    public String sealingContext() {
        return sealingContextFor(name);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNoradId() {
        return noradId;
    }

    public MissionPhase getMissionPhase() {
        return missionPhase;
    }

    public byte[] getTelemetryKeySealed() {
        return telemetryKeySealed.clone();
    }

    public byte[] getCommandPublicKey() {
        return commandPublicKey.clone();
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
