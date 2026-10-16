package io.github.justhurky.groundsegment.spacecraft.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSpacecraftRequest(
        @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[A-Za-z0-9 _-]+") String name,
        @Min(1) @Max(99999) int noradId,
        @NotBlank @Size(max = 512) String commandPublicKeyBase64) {
}
