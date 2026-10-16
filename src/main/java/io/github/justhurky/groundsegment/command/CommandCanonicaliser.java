package io.github.justhurky.groundsegment.command;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CommandCanonicaliser {

    public static final String FIELD_SEPARATOR = "\n";
    public static final String PARAMETER_SEPARATOR = ";";
    public static final String PARAMETER_ASSIGNMENT = "=";

    public String canonicalParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        return new TreeMap<>(parameters).entrySet().stream()
                .map(entry -> entry.getKey() + PARAMETER_ASSIGNMENT + entry.getValue())
                .collect(Collectors.joining(PARAMETER_SEPARATOR));
    }

    public String canonicalise(UUID spacecraftId, String opcode, CommandCriticality criticality, String nonce,
                               Instant requestedAt, Map<String, String> parameters) {
        return String.join(FIELD_SEPARATOR,
                spacecraftId.toString(),
                CommandCatalogue.normalise(opcode),
                criticality.name(),
                nonce,
                Long.toString(requestedAt.toEpochMilli()),
                canonicalParameters(parameters));
    }

    public byte[] signedBytes(String canonicalForm) {
        return canonicalForm.getBytes(StandardCharsets.UTF_8);
    }
}
