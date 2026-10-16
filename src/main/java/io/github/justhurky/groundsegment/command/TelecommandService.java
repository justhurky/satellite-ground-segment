package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.command.dto.SubmitCommandRequest;
import io.github.justhurky.groundsegment.command.dto.TelecommandResponse;
import io.github.justhurky.groundsegment.common.error.DuplicateResourceException;
import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.common.util.Digests;
import io.github.justhurky.groundsegment.crypto.CommandSignatureVerifier;
import io.github.justhurky.groundsegment.identity.CurrentActor;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.telemetry.CommandAcknowledgementReceived;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelecommandService {

    private static final Logger log = LoggerFactory.getLogger(TelecommandService.class);

    private final TelecommandRepository telecommandRepository;
    private final SpacecraftService spacecraftService;
    private final CommandAuthorisationService authorisationService;
    private final CommandCanonicaliser canonicaliser;
    private final CommandSignatureVerifier signatureVerifier;
    private final AuditService auditService;
    private final CurrentActor currentActor;
    private final CommandMetrics metrics;
    private final Clock clock;

    public TelecommandService(TelecommandRepository telecommandRepository,
                              SpacecraftService spacecraftService,
                              CommandAuthorisationService authorisationService,
                              CommandCanonicaliser canonicaliser,
                              CommandSignatureVerifier signatureVerifier,
                              AuditService auditService,
                              CurrentActor currentActor,
                              CommandMetrics metrics,
                              Clock clock) {
        this.telecommandRepository = telecommandRepository;
        this.spacecraftService = spacecraftService;
        this.authorisationService = authorisationService;
        this.canonicaliser = canonicaliser;
        this.signatureVerifier = signatureVerifier;
        this.auditService = auditService;
        this.currentActor = currentActor;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public TelecommandResponse submit(UUID spacecraftId, SubmitCommandRequest request) {
        Spacecraft spacecraft = spacecraftService.require(spacecraftId);
        if (telecommandRepository.existsBySpacecraftIdAndNonce(spacecraftId, request.nonce())) {
            throw new DuplicateResourceException("Nonce " + request.nonce()
                    + " was already used for this spacecraft");
        }
        Instant now = clock.instant();
        CommandCatalogue.Rule rule = authorisationService.authorise(spacecraft, request, now);

        String canonicalForm = canonicaliser.canonicalise(spacecraftId, request.opcode(), request.criticality(),
                request.nonce(), request.requestedAt(), request.parameters());
        byte[] signedBytes = canonicaliser.signedBytes(canonicalForm);
        byte[] signature = decodeSignature(request.signatureBase64());

        Telecommand command = new Telecommand(spacecraft, CommandCatalogue.normalise(request.opcode()),
                request.criticality(), canonicaliser.canonicalParameters(request.parameters()), request.nonce(),
                request.requestedAt(), now, request.executeAfter(), signature,
                Digests.sha256Hex(signedBytes), rule.payloadBytes(), currentActor.actorName());

        if (!signatureVerifier.verify(spacecraft.getCommandPublicKey(), signedBytes, signature)) {
            command.reject("signature-verification-failed");
            Telecommand rejected = telecommandRepository.save(command);
            metrics.rejected("signature-verification-failed");
            auditService.record(AuditAction.COMMAND_REJECTED, "telecommand", rejected.getId().toString(),
                    Map.of("opcode", rejected.getOpcode(), "reason", "signature-verification-failed"));
            return TelecommandResponse.from(rejected);
        }

        command.markValidated();
        Telecommand validated = telecommandRepository.save(command);
        metrics.submitted(validated.getCriticality());
        auditService.record(AuditAction.COMMAND_SUBMITTED, "telecommand", validated.getId().toString(),
                Map.of("opcode", validated.getOpcode(),
                        "criticality", validated.getCriticality().name(),
                        "payloadDigest", validated.getPayloadDigest()));
        return TelecommandResponse.from(validated);
    }

    @Transactional
    public TelecommandResponse cancel(UUID commandId, String reason) {
        Telecommand command = require(commandId);
        command.cancel(reason, clock.instant());
        metrics.completed(CommandState.CANCELLED);
        auditService.record(AuditAction.COMMAND_CANCELLED, "telecommand", commandId.toString(),
                Map.of("opcode", command.getOpcode(), "reason", reason));
        return TelecommandResponse.from(command);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyAcknowledgement(CommandAcknowledgementReceived acknowledgement) {
        telecommandRepository.findById(acknowledgement.commandId()).ifPresentOrElse(
                command -> applyTo(command, acknowledgement),
                () -> log.warn("Received an acknowledgement for unknown command {} on frame {}",
                        acknowledgement.commandId(), acknowledgement.frameId()));
    }

    private void applyTo(Telecommand command, CommandAcknowledgementReceived acknowledgement) {
        if (command.getState() != CommandState.UPLINKED) {
            log.warn("Ignoring acknowledgement for command {} because it is in state {}",
                    command.getId(), command.getState());
            return;
        }
        if (acknowledgement.executed()) {
            command.markAcknowledged(acknowledgement.resultCode(), acknowledgement.receivedAt());
            metrics.completed(CommandState.ACKNOWLEDGED);
            auditService.record(AuditAction.COMMAND_ACKNOWLEDGED, "telecommand", command.getId().toString(),
                    Map.of("opcode", command.getOpcode(), "frameId", Long.toString(acknowledgement.frameId())));
            return;
        }
        command.fail(acknowledgement.resultCode(), "spacecraft-reported-failure", acknowledgement.receivedAt());
        metrics.completed(CommandState.FAILED);
        auditService.record(AuditAction.COMMAND_FAILED, "telecommand", command.getId().toString(),
                Map.of("opcode", command.getOpcode(),
                        "resultCode", Integer.toString(acknowledgement.resultCode()),
                        "frameId", Long.toString(acknowledgement.frameId())));
    }

    @Transactional(readOnly = true)
    public TelecommandResponse findById(UUID commandId) {
        return TelecommandResponse.from(require(commandId));
    }

    @Transactional(readOnly = true)
    public Telecommand require(UUID commandId) {
        return telecommandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("telecommand", commandId));
    }

    @Transactional(readOnly = true)
    public Page<TelecommandResponse> search(UUID spacecraftId, CommandState state, Pageable pageable) {
        spacecraftService.require(spacecraftId);
        return telecommandRepository.search(spacecraftId, state, pageable).map(TelecommandResponse::from);
    }

    private byte[] decodeSignature(String signatureBase64) {
        try {
            return Codecs.fromBase64(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("signatureBase64 is not valid base64");
        }
    }
}
