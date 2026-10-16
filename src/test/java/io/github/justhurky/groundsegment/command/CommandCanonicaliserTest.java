package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommandCanonicaliserTest {

    private static final UUID SPACECRAFT_ID = UUID.fromString("2d9e4b50-7f1a-4c6e-8f2a-8f6a7c1d9e30");
    private static final Instant REQUESTED_AT = Instant.parse("2026-03-14T09:26:53.589Z");

    private final CommandCanonicaliser canonicaliser = new CommandCanonicaliser();

    @Test
    void ordersParametersByKeyRegardlessOfInsertionOrder() {
        Map<String, String> inserted = new LinkedHashMap<>();
        inserted.put("thrust", "0.4");
        inserted.put("axis", "z");
        inserted.put("duration", "12");

        assertThat(canonicaliser.canonicalParameters(inserted)).isEqualTo("axis=z;duration=12;thrust=0.4");
    }

    @Test
    void rendersAnEmptyParameterSetAsAnEmptyString() {
        assertThat(canonicaliser.canonicalParameters(Map.of())).isEmpty();
        assertThat(canonicaliser.canonicalParameters(null)).isEmpty();
    }

    @Test
    void buildsAStableCanonicalForm() {
        String canonical = canonicaliser.canonicalise(SPACECRAFT_ID, "thruster-burn",
                CommandCriticality.CRITICAL, "nonce-0123456789abcdef", REQUESTED_AT,
                Map.of("duration", "12"));

        assertThat(canonical).isEqualTo(SPACECRAFT_ID + "\nTHRUSTER_BURN\nCRITICAL\nnonce-0123456789abcdef\n"
                + REQUESTED_AT.toEpochMilli() + "\nduration=12");
        assertThat(canonical).doesNotContain("2026-03-14");
    }

    @Test
    void isIndependentOfOpcodeSpellingAndSeparators() {
        String hyphenated = canonicaliser.canonicalise(SPACECRAFT_ID, "thruster-burn",
                CommandCriticality.CRITICAL, "nonce-0123456789abcdef", REQUESTED_AT, Map.of());
        String underscored = canonicaliser.canonicalise(SPACECRAFT_ID, "THRUSTER_BURN",
                CommandCriticality.CRITICAL, "nonce-0123456789abcdef", REQUESTED_AT, Map.of());

        assertThat(hyphenated).isEqualTo(underscored);
    }

    @Test
    void changesWhenTheCriticalityIsDowngraded() {
        String critical = canonicaliser.canonicalise(SPACECRAFT_ID, "THRUSTER_BURN",
                CommandCriticality.CRITICAL, "nonce-0123456789abcdef", REQUESTED_AT, Map.of());
        String routine = canonicaliser.canonicalise(SPACECRAFT_ID, "THRUSTER_BURN",
                CommandCriticality.ROUTINE, "nonce-0123456789abcdef", REQUESTED_AT, Map.of());

        assertThat(critical).isNotEqualTo(routine);
    }

    @Test
    void encodesTheCanonicalFormAsUtfEightBytes() {
        assertThat(canonicaliser.signedBytes("PING")).containsExactly('P', 'I', 'N', 'G');
    }
}
