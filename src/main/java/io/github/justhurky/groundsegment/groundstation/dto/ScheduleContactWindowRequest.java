package io.github.justhurky.groundsegment.groundstation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ScheduleContactWindowRequest(
        @NotBlank String groundStationCode,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Min(1) @Max(1048576) int uplinkBudgetBytes) {
}
