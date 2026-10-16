package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import io.github.justhurky.groundsegment.crypto.FrameAuthenticator;
import io.github.justhurky.groundsegment.crypto.PayloadSealer;
import io.github.justhurky.groundsegment.groundstation.GroundStation;
import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.spacecraft.ParameterDefinition;
import io.github.justhurky.groundsegment.spacecraft.ParameterDefinitionRepository;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.telemetry.dto.AlertResponse;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestRequest;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryIngestService {

    private final SpacecraftService spacecraftService;
    private final GroundStationService groundStationService;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final TelemetryFrameRepository telemetryFrameRepository;
    private final AlertRepository alertRepository;
    private final CcsdsFrameDecoder frameDecoder;
    private final FrameAuthenticator frameAuthenticator;
    private final PayloadSealer payloadSealer;
    private final ReplayGuard replayGuard;
    private final LimitChecker limitChecker;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final TelemetryMetrics metrics;
    private final int maxFrameBytes;
    private final int acknowledgementApid;
    private final Clock clock;

    public TelemetryIngestService(SpacecraftService spacecraftService,
                                  GroundStationService groundStationService,
                                  ParameterDefinitionRepository parameterDefinitionRepository,
                                  TelemetryFrameRepository telemetryFrameRepository,
                                  AlertRepository alertRepository,
                                  CcsdsFrameDecoder frameDecoder,
                                  FrameAuthenticator frameAuthenticator,
                                  PayloadSealer payloadSealer,
                                  ReplayGuard replayGuard,
                                  LimitChecker limitChecker,
                                  AuditService auditService,
                                  ApplicationEventPublisher eventPublisher,
                                  TelemetryMetrics metrics,
                                  GroundSegmentProperties properties,
                                  Clock clock) {
        this.spacecraftService = spacecraftService;
        this.groundStationService = groundStationService;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
        this.telemetryFrameRepository = telemetryFrameRepository;
        this.alertRepository = alertRepository;
        this.frameDecoder = frameDecoder;
        this.frameAuthenticator = frameAuthenticator;
        this.payloadSealer = payloadSealer;
        this.replayGuard = replayGuard;
        this.limitChecker = limitChecker;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.maxFrameBytes = properties.telemetry().maxFrameBytes();
        this.acknowledgementApid = properties.telemetry().acknowledgementApid();
        this.clock = clock;
    }

    @Transactional
    public FrameIngestResponse ingest(FrameIngestRequest request) {
        Spacecraft spacecraft = spacecraftService.require(request.spacecraftId());
        GroundStation station = groundStationService.requireByCode(request.groundStationCode());

        byte[] frameBytes = decodeBase64(request.frameBase64(), "frameBase64");
        if (frameBytes.length > maxFrameBytes) {
            reject(spacecraft, station, "frame-too-large");
            throw new FrameFormatException("Frame of " + frameBytes.length
                    + " bytes exceeds the configured maximum of " + maxFrameBytes);
        }
        byte[] presentedTag = decodeBase64(request.authenticationTagBase64(), "authenticationTagBase64");
        if (!frameAuthenticator.matches(spacecraftService.telemetryKeyOf(spacecraft), frameBytes, presentedTag)) {
            reject(spacecraft, station, "authentication-tag-mismatch");
            throw new FrameAuthenticationException(
                    "Frame authentication tag does not match the registered spacecraft key");
        }

        DecodedFrame decoded = decodeFrame(spacecraft, station, frameBytes);
        replayGuard.admit(spacecraft.getId(), decoded.apid(), decoded.sequenceCount());

        Instant receivedAt = clock.instant();
        byte[] sealedPayload = payloadSealer.seal(decoded.payload(),
                TelemetryFrame.sealingContextFor(spacecraft, decoded.apid()));
        TelemetryFrame frame = new TelemetryFrame(spacecraft, station, decoded, sealedPayload, receivedAt);

        if (decoded.apid() == acknowledgementApid) {
            return ingestAcknowledgement(frame, decoded, receivedAt);
        }
        return ingestParameters(frame, decoded, receivedAt);
    }

    private FrameIngestResponse ingestAcknowledgement(TelemetryFrame frame, DecodedFrame decoded,
                                                      Instant receivedAt) {
        CommandAcknowledgement acknowledgement = CommandAcknowledgement.decode(decoded.payload());
        TelemetryFrame saved = telemetryFrameRepository.save(frame);
        eventPublisher.publishEvent(new CommandAcknowledgementReceived(
                acknowledgement.commandId(), acknowledgement.executed(), acknowledgement.resultCode(),
                saved.getId(), receivedAt));
        accept(saved, decoded, 0);
        return new FrameIngestResponse(saved.getId(), saved.getSpacecraft().getId(), decoded.apid(),
                decoded.sequenceCount(), decoded.generatedAt(), receivedAt, 0, 0, List.of(),
                acknowledgement.commandId());
    }

    private FrameIngestResponse ingestParameters(TelemetryFrame frame, DecodedFrame decoded, Instant receivedAt) {
        List<ParameterSample> samples = frameDecoder.decodeSamples(decoded.payload());
        Map<Integer, ParameterDefinition> definitions = parameterDefinitionRepository
                .findAllBySpacecraftIdOrderByParameterSlotAsc(frame.getSpacecraft().getId()).stream()
                .collect(Collectors.toMap(ParameterDefinition::getParameterSlot, Function.identity()));

        List<PendingBreach> breaches = new ArrayList<>();
        int unmappedSamples = 0;
        for (ParameterSample sample : samples) {
            ParameterDefinition definition = definitions.get(sample.parameterSlot());
            if (definition == null) {
                frame.addSample(new TelemetrySample(frame, sample.parameterSlot(),
                        TelemetrySample.unmappedName(sample.parameterSlot()), sample.value(), "",
                        false, decoded.generatedAt()));
                unmappedSamples++;
                continue;
            }
            frame.addSample(new TelemetrySample(frame, sample.parameterSlot(), definition.getName(),
                    sample.value(), definition.getUnit(), true, decoded.generatedAt()));
            limitChecker.evaluate(definition, sample.value())
                    .ifPresent(breach -> breaches.add(
                            new PendingBreach(definition.getName(), sample.value(), breach)));
        }

        TelemetryFrame saved = telemetryFrameRepository.save(frame);
        List<AlertResponse> raised = breaches.stream()
                .map(pending -> raiseAlert(saved, pending, receivedAt))
                .toList();
        accept(saved, decoded, samples.size());

        return new FrameIngestResponse(saved.getId(), saved.getSpacecraft().getId(), decoded.apid(),
                decoded.sequenceCount(), decoded.generatedAt(), receivedAt, samples.size(), unmappedSamples,
                raised, null);
    }

    private AlertResponse raiseAlert(TelemetryFrame frame, PendingBreach pending, Instant raisedAt) {
        Alert alert = alertRepository.save(new Alert(frame.getSpacecraft(), frame.getId(),
                pending.parameterName(), pending.value(), pending.breach(), raisedAt));
        metrics.alertRaised(pending.breach().severity());
        auditService.record(AuditAction.LIMIT_BREACH_RAISED, "alert", alert.getId().toString(),
                Map.of("spacecraftId", frame.getSpacecraft().getId().toString(),
                        "parameter", pending.parameterName(),
                        "severity", pending.breach().severity().name(),
                        "limitKind", pending.breach().limitKind().name(),
                        "observedValue", Double.toString(pending.value())));
        return AlertResponse.from(alert);
    }

    private DecodedFrame decodeFrame(Spacecraft spacecraft, GroundStation station, byte[] frameBytes) {
        try {
            return frameDecoder.decode(frameBytes);
        } catch (FrameFormatException e) {
            reject(spacecraft, station, "malformed-frame");
            throw e;
        }
    }

    private void accept(TelemetryFrame frame, DecodedFrame decoded, int sampleCount) {
        metrics.frameAccepted(decoded.apid());
        Map<String, String> details = new HashMap<>();
        details.put("apid", Integer.toString(decoded.apid()));
        details.put("sequenceCount", Integer.toString(decoded.sequenceCount()));
        details.put("sampleCount", Integer.toString(sampleCount));
        details.put("groundStation", frame.getGroundStation().getCode());
        auditService.record(AuditAction.TELEMETRY_FRAME_ACCEPTED, "telemetry-frame",
                Optional.ofNullable(frame.getId()).map(String::valueOf).orElse(""), details);
    }

    private void reject(Spacecraft spacecraft, GroundStation station, String reason) {
        metrics.frameRejected(reason);
        auditService.recordIndependently(AuditAction.TELEMETRY_FRAME_REJECTED, "spacecraft",
                spacecraft.getId().toString(),
                Map.of("reason", reason, "groundStation", station.getCode()));
    }

    private byte[] decodeBase64(String value, String field) {
        try {
            return Codecs.fromBase64(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " is not valid base64");
        }
    }

    private record PendingBreach(String parameterName, double value, LimitBreach breach) {
    }
}
