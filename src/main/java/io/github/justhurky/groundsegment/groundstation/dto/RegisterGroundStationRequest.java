package io.github.justhurky.groundsegment.groundstation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterGroundStationRequest(
        @NotBlank @Size(min = 2, max = 16) @Pattern(regexp = "[A-Z0-9-]+") String code,
        @NotBlank @Size(max = 64) String name,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitudeDegrees,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitudeDegrees,
        @DecimalMin("-500.0") @DecimalMax("9000.0") double altitudeMetres) {
}
