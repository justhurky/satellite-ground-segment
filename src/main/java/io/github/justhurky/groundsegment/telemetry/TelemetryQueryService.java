package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.telemetry.dto.TelemetryFrameResponse;
import io.github.justhurky.groundsegment.telemetry.dto.TelemetrySampleResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryQueryService {

    private final SpacecraftService spacecraftService;
    private final TelemetryFrameRepository telemetryFrameRepository;
    private final TelemetrySampleRepository telemetrySampleRepository;

    public TelemetryQueryService(SpacecraftService spacecraftService,
                                 TelemetryFrameRepository telemetryFrameRepository,
                                 TelemetrySampleRepository telemetrySampleRepository) {
        this.spacecraftService = spacecraftService;
        this.telemetryFrameRepository = telemetryFrameRepository;
        this.telemetrySampleRepository = telemetrySampleRepository;
    }

    @Transactional(readOnly = true)
    public Page<TelemetryFrameResponse> frames(UUID spacecraftId, Integer apid, Instant from, Instant to,
                                               Pageable pageable) {
        spacecraftService.require(spacecraftId);
        return telemetryFrameRepository.search(spacecraftId, apid, from, to, pageable)
                .map(TelemetryFrameResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TelemetrySampleResponse> series(UUID spacecraftId, String parameterName, Instant from,
                                               Instant to, int limit) {
        spacecraftService.require(spacecraftId);
        return telemetrySampleRepository.findSeries(spacecraftId, parameterName, from, to,
                        PageRequest.of(0, limit)).stream()
                .map(TelemetrySampleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TelemetrySampleResponse> samplesOf(long frameId) {
        return telemetryFrameRepository.findById(frameId)
                .map(frame -> frame.getSamples().stream().map(TelemetrySampleResponse::from).toList())
                .orElseThrow(() -> new ResourceNotFoundException("telemetry-frame", frameId));
    }
}
