package io.github.justhurky.groundsegment.identity.dto;

import io.github.justhurky.groundsegment.identity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[a-z0-9._-]+") String username,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotEmpty Set<Role> roles) {
}
