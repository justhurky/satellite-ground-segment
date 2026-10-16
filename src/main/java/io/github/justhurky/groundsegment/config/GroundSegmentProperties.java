package io.github.justhurky.groundsegment.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ground-segment")
public record GroundSegmentProperties(
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid Crypto crypto,
        @NotNull @Valid Telemetry telemetry,
        @NotNull @Valid Uplink uplink) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotNull Duration accessTokenTtl,
            @NotBlank String issuer) {
    }

    public record Crypto(@NotBlank String dataEncryptionKey) {
    }

    public record Telemetry(
            @Min(1) @Max(64) int replayWindowSize,
            @Min(16) @Max(65536) int maxFrameBytes,
            @Min(0) @Max(2047) int acknowledgementApid) {
    }

    public record Uplink(
            @Min(1) @Max(1000) int maxCommandsPerPass,
            @NotNull Duration maxClockSkew) {
    }
}
