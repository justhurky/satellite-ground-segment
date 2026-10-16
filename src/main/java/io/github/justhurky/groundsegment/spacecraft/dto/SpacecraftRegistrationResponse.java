package io.github.justhurky.groundsegment.spacecraft.dto;

public record SpacecraftRegistrationResponse(
        SpacecraftResponse spacecraft,
        String telemetryAuthenticationKeyBase64) {
}
