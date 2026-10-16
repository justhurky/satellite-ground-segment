package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandCatalogueTest {

    private final CommandCatalogue catalogue = new CommandCatalogue(Map.of(
            "ping", new CommandCatalogue.Rule(CommandCriticality.ROUTINE,
                    EnumSet.of(MissionPhase.NOMINAL), Role.OPERATOR, 16),
            "thruster-burn", new CommandCatalogue.Rule(CommandCriticality.CRITICAL,
                    EnumSet.of(MissionPhase.NOMINAL), Role.FLIGHT_DIRECTOR, 64)));

    @Test
    void normalisesConfiguredKeysToUpperCaseWithUnderscores() {
        assertThat(catalogue.opcodes()).containsExactlyInAnyOrder("PING", "THRUSTER_BURN");
    }

    @Test
    void resolvesAnOpcodeIrrespectiveOfCaseAndSeparator() {
        assertThat(catalogue.ruleFor("thruster_burn")).isPresent();
        assertThat(catalogue.ruleFor("THRUSTER-BURN")).isPresent();
        assertThat(catalogue.ruleFor(" Thruster-Burn ")).isPresent();
    }

    @Test
    void returnsTheConfiguredRule() {
        CommandCatalogue.Rule rule = catalogue.ruleFor("THRUSTER_BURN").orElseThrow();

        assertThat(rule.criticality()).isEqualTo(CommandCriticality.CRITICAL);
        assertThat(rule.requiredRole()).isEqualTo(Role.FLIGHT_DIRECTOR);
        assertThat(rule.payloadBytes()).isEqualTo(64);
        assertThat(rule.allowedPhases()).containsExactly(MissionPhase.NOMINAL);
    }

    @Test
    void reportsAnUnknownOpcodeAsAbsent() {
        assertThat(catalogue.ruleFor("SELF_DESTRUCT")).isEmpty();
    }
}
