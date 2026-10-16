package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.common.api.PageResponse;
import io.github.justhurky.groundsegment.telemetry.dto.AlertResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/spacecraft/{spacecraftId}/alerts")
    @PreAuthorize("hasAnyRole('ANALYST','OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public PageResponse<AlertResponse> alerts(
            @PathVariable UUID spacecraftId,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return PageResponse.of(
                alertService.search(spacecraftId, severity, acknowledged, PageRequest.of(page, size)));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public AlertResponse acknowledge(@PathVariable UUID alertId) {
        return alertService.acknowledge(alertId);
    }
}
