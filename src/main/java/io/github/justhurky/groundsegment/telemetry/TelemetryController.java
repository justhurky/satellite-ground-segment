package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.common.api.PageResponse;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestRequest;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestResponse;
import io.github.justhurky.groundsegment.telemetry.dto.TelemetryFrameResponse;
import io.github.justhurky.groundsegment.telemetry.dto.TelemetrySampleResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class TelemetryController {

    private final TelemetryIngestService ingestService;
    private final TelemetryQueryService queryService;

    public TelemetryController(TelemetryIngestService ingestService, TelemetryQueryService queryService) {
        this.ingestService = ingestService;
        this.queryService = queryService;
    }

    @PostMapping("/telemetry/frames")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('GROUND_STATION','ADMIN')")
    public FrameIngestResponse ingest(@Valid @RequestBody FrameIngestRequest request) {
        return ingestService.ingest(request);
    }

    @GetMapping("/spacecraft/{spacecraftId}/telemetry/frames")
    @PreAuthorize("hasAnyRole('ANALYST','OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public PageResponse<TelemetryFrameResponse> frames(
            @PathVariable UUID spacecraftId,
            @RequestParam(required = false) Integer apid,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return PageResponse.of(queryService.frames(spacecraftId, apid, from, to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"))));
    }

    @GetMapping("/telemetry/frames/{frameId}/samples")
    @PreAuthorize("hasAnyRole('ANALYST','OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public List<TelemetrySampleResponse> samples(@PathVariable long frameId) {
        return queryService.samplesOf(frameId);
    }

    @GetMapping("/spacecraft/{spacecraftId}/telemetry/parameters/{parameterName}/series")
    @PreAuthorize("hasAnyRole('ANALYST','OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public List<TelemetrySampleResponse> series(
            @PathVariable UUID spacecraftId,
            @PathVariable String parameterName,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "500") @Min(1) @Max(5000) int limit) {
        return queryService.series(spacecraftId, parameterName, from, to, limit);
    }
}
