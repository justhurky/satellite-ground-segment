package io.github.justhurky.groundsegment.spacecraft;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.common.error.DuplicateResourceException;
import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.crypto.CommandSignatureVerifier;
import io.github.justhurky.groundsegment.crypto.FrameAuthenticator;
import io.github.justhurky.groundsegment.crypto.PayloadSealer;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionResponse;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftRegistrationResponse;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpacecraftService {

    private static final byte[] KEY_USABILITY_PROBE =
            "ground-segment-key-probe".getBytes(StandardCharsets.UTF_8);
    private static final int ED25519_SIGNATURE_LENGTH = 64;

    private final SpacecraftRepository spacecraftRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final PayloadSealer payloadSealer;
    private final CommandSignatureVerifier signatureVerifier;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SpacecraftService(SpacecraftRepository spacecraftRepository,
                             ParameterDefinitionRepository parameterDefinitionRepository,
                             PayloadSealer payloadSealer,
                             CommandSignatureVerifier signatureVerifier,
                             AuditService auditService,
                             Clock clock) {
        this.spacecraftRepository = spacecraftRepository;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
        this.payloadSealer = payloadSealer;
        this.signatureVerifier = signatureVerifier;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public SpacecraftRegistrationResponse register(RegisterSpacecraftRequest request) {
        if (spacecraftRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("A spacecraft named " + request.name() + " is already registered");
        }
        if (spacecraftRepository.existsByNoradId(request.noradId())) {
            throw new DuplicateResourceException("NORAD id " + request.noradId() + " is already registered");
        }
        byte[] commandPublicKey = readCommandPublicKey(request.commandPublicKeyBase64());
        byte[] telemetryKey = new byte[FrameAuthenticator.KEY_LENGTH_BYTES];
        random.nextBytes(telemetryKey);

        byte[] sealedKey = payloadSealer.seal(telemetryKey, Spacecraft.sealingContextFor(request.name()));
        Spacecraft spacecraft = spacecraftRepository.save(new Spacecraft(request.name(), request.noradId(),
                sealedKey, commandPublicKey, clock.instant()));

        auditService.record(AuditAction.SPACECRAFT_REGISTERED, "spacecraft", spacecraft.getId().toString(),
                Map.of("name", spacecraft.getName(), "noradId", Integer.toString(spacecraft.getNoradId())));

        return new SpacecraftRegistrationResponse(SpacecraftResponse.from(spacecraft),
                Codecs.toBase64(telemetryKey));
    }

    @Transactional(readOnly = true)
    public List<SpacecraftResponse> findAll() {
        return spacecraftRepository.findAll(Sort.by("name")).stream()
                .map(SpacecraftResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpacecraftResponse findById(UUID spacecraftId) {
        return SpacecraftResponse.from(require(spacecraftId));
    }

    @Transactional(readOnly = true)
    public Spacecraft require(UUID spacecraftId) {
        return spacecraftRepository.findById(spacecraftId)
                .orElseThrow(() -> new ResourceNotFoundException("spacecraft", spacecraftId));
    }

    @Transactional(readOnly = true)
    public byte[] telemetryKeyOf(Spacecraft spacecraft) {
        return payloadSealer.unseal(spacecraft.getTelemetryKeySealed(), spacecraft.sealingContext());
    }

    @Transactional
    public SpacecraftResponse changeMissionPhase(UUID spacecraftId, MissionPhase targetPhase, String reason) {
        Spacecraft spacecraft = require(spacecraftId);
        MissionPhase previousPhase = spacecraft.getMissionPhase();
        spacecraft.transitionTo(targetPhase);
        auditService.record(AuditAction.MISSION_PHASE_CHANGED, "spacecraft", spacecraftId.toString(),
                Map.of("from", previousPhase.name(), "to", targetPhase.name(), "reason", reason));
        return SpacecraftResponse.from(spacecraft);
    }

    @Transactional
    public ParameterDefinitionResponse defineParameter(UUID spacecraftId, ParameterDefinitionRequest request) {
        Spacecraft spacecraft = require(spacecraftId);
        parameterDefinitionRepository.findBySpacecraftIdAndName(spacecraftId, request.name())
                .filter(existing -> existing.getParameterSlot() != request.parameterSlot())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Parameter " + request.name()
                            + " is already defined on slot " + existing.getParameterSlot());
                });

        ParameterDefinition definition = parameterDefinitionRepository
                .findBySpacecraftIdAndParameterSlot(spacecraftId, request.parameterSlot())
                .orElseGet(() -> new ParameterDefinition(spacecraft, request.parameterSlot(),
                        request.name(), request.unit(), null, null, null, null));
        definition.applyLimits(request.redLow(), request.yellowLow(), request.yellowHigh(), request.redHigh());
        ParameterDefinition saved = parameterDefinitionRepository.save(definition);

        auditService.record(AuditAction.PARAMETER_LIMITS_DEFINED, "parameter-definition", saved.getId().toString(),
                Map.of("spacecraftId", spacecraftId.toString(), "parameter", saved.getName()));
        return ParameterDefinitionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ParameterDefinitionResponse> parametersOf(UUID spacecraftId) {
        require(spacecraftId);
        return parameterDefinitionRepository.findAllBySpacecraftIdOrderByParameterSlotAsc(spacecraftId).stream()
                .map(ParameterDefinitionResponse::from)
                .toList();
    }

    private byte[] readCommandPublicKey(String base64) {
        byte[] key;
        try {
            key = Codecs.fromBase64(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("commandPublicKeyBase64 is not valid base64");
        }
        signatureVerifier.verify(key, KEY_USABILITY_PROBE, new byte[ED25519_SIGNATURE_LENGTH]);
        return key;
    }
}
