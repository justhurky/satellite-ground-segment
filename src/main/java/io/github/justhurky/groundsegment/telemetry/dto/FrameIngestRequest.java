package io.github.justhurky.groundsegment.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FrameIngestRequest(
        @NotNull UUID spacecraftId,
        @NotBlank @Size(max = 16) String groundStationCode,
        @NotBlank @Size(max = 131072) String frameBase64,
        @NotBlank @Size(max = 64) String authenticationTagBase64) {
}
