package io.github.justhurky.groundsegment.spacecraft.dto;

import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpacecraftResponse(
        UUID id,
        String name,
        int noradId,
        MissionPhase missionPhase,
        List<MissionPhase> allowedPhaseTargets,
        String commandPublicKeyBase64,
        Instant registeredAt) {

    public static SpacecraftResponse from(Spacecraft spacecraft) {
        return new SpacecraftResponse(
                spacecraft.getId(),
                spacecraft.getName(),
                spacecraft.getNoradId(),
                spacecraft.getMissionPhase(),
                List.copyOf(spacecraft.getMissionPhase().allowedTargets()),
                Codecs.toBase64(spacecraft.getCommandPublicKey()),
                spacecraft.getRegisteredAt());
    }
}
