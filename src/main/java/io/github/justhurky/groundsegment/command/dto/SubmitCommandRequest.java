package io.github.justhurky.groundsegment.command.dto;

import io.github.justhurky.groundsegment.command.CommandCriticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public record SubmitCommandRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String opcode,
        @NotNull CommandCriticality criticality,
        @Size(max = 32) Map<@NotBlank @Size(max = 32) String, @NotNull @Size(max = 64) String> parameters,
        @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String nonce,
        @NotNull Instant requestedAt,
        Instant executeAfter,
        @NotBlank @Size(max = 256) String signatureBase64) {
}
