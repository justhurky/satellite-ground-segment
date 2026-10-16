package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ground-segment.commands")
public record CommandCatalogue(@NotEmpty Map<String, @Valid Rule> catalogue) {

    public CommandCatalogue(Map<String, Rule> catalogue) {
        Map<String, Rule> normalised = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        catalogue.forEach((opcode, rule) -> normalised.put(normalise(opcode), rule));
        this.catalogue = Collections.unmodifiableMap(normalised);
    }

    public static String normalise(String opcode) {
        return opcode.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    public Optional<Rule> ruleFor(String opcode) {
        return Optional.ofNullable(catalogue.get(normalise(opcode)));
    }

    public Set<String> opcodes() {
        return catalogue.keySet();
    }

    public record Rule(
            @NotNull CommandCriticality criticality,
            @NotEmpty Set<MissionPhase> allowedPhases,
            @NotNull Role requiredRole,
            @Min(1) @Max(4096) int payloadBytes) {
    }
}
