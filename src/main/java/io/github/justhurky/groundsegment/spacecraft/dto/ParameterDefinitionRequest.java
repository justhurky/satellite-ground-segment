package io.github.justhurky.groundsegment.spacecraft.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParameterDefinitionRequest(
        @Min(0) @Max(255) int parameterSlot,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z0-9_]+") String name,
        @NotBlank @Size(max = 16) String unit,
        Double redLow,
        Double yellowLow,
        Double yellowHigh,
        Double redHigh) {
}
