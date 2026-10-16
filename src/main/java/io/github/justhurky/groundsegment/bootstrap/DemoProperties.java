package io.github.justhurky.groundsegment.bootstrap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Validated
@Profile(DemoDataSeeder.DEMO_PROFILE)
@ConfigurationProperties(prefix = "ground-segment.demo")
public record DemoProperties(
        @NotBlank @Size(min = 12) String password,
        @NotBlank String commandPublicKeyBase64,
        @NotBlank String commandPrivateKeyBase64) {
}
