package io.github.justhurky.groundsegment.spacecraft.dto;

import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MissionPhaseChangeRequest(
        @NotNull MissionPhase targetPhase,
        @NotBlank @Size(max = 256) String reason) {
}
