package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.identity.CurrentActor;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.telemetry.dto.AlertResponse;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final SpacecraftService spacecraftService;
    private final AuditService auditService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public AlertService(AlertRepository alertRepository, SpacecraftService spacecraftService,
                        AuditService auditService, CurrentActor currentActor, Clock clock) {
        this.alertRepository = alertRepository;
        this.spacecraftService = spacecraftService;
        this.auditService = auditService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> search(UUID spacecraftId, AlertSeverity severity, Boolean acknowledged,
                                      Pageable pageable) {
        spacecraftService.require(spacecraftId);
        return alertRepository.search(spacecraftId, severity, acknowledged, pageable).map(AlertResponse::from);
    }

    @Transactional
    public AlertResponse acknowledge(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("alert", alertId));
        alert.acknowledge(currentActor.actorName(), clock.instant());
        auditService.record(AuditAction.ALERT_ACKNOWLEDGED, "alert", alertId.toString(),
                Map.of("parameter", alert.getParameterName(), "severity", alert.getSeverity().name()));
        return AlertResponse.from(alert);
    }
}
