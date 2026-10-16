package io.github.justhurky.groundsegment.command.dto;

import io.github.justhurky.groundsegment.command.CommandCriticality;
import io.github.justhurky.groundsegment.command.CommandState;
import io.github.justhurky.groundsegment.command.Telecommand;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TelecommandResponse(
        UUID id,
        UUID spacecraftId,
        String opcode,
        CommandCriticality criticality,
        CommandState state,
        List<CommandState> allowedNextStates,
        String canonicalParameters,
        String payloadDigest,
        String nonce,
        String requestedBy,
        Instant requestedAt,
        Instant submittedAt,
        Instant executeAfter,
        UUID contactWindowId,
        Instant scheduledAt,
        Instant uplinkedAt,
        Instant completedAt,
        Integer resultCode,
        String outcomeDetail) {

    public static TelecommandResponse from(Telecommand command) {
        return new TelecommandResponse(
                command.getId(),
                command.getSpacecraft().getId(),
                command.getOpcode(),
                command.getCriticality(),
                command.getState(),
                List.copyOf(command.getState().allowedTargets()),
                command.getCanonicalParameters(),
                command.getPayloadDigest(),
                command.getNonce(),
                command.getRequestedBy(),
                command.getRequestedAt(),
                command.getSubmittedAt(),
                command.getExecuteAfter(),
                command.getContactWindowId(),
                command.getScheduledAt(),
                command.getUplinkedAt(),
                command.getCompletedAt(),
                command.getResultCode(),
                command.getOutcomeDetail());
    }
}
