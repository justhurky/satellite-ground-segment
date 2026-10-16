package io.github.justhurky.groundsegment.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelCommandRequest(@NotBlank @Size(max = 256) String reason) {
}
