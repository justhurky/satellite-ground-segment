package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import io.github.justhurky.groundsegment.groundstation.ContactWindow;
import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.identity.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UplinkPassService {

    private final TelecommandRepository telecommandRepository;
    private final GroundStationService groundStationService;
    private final UplinkChannel uplinkChannel;
    private final AuditService auditService;
    private final CommandMetrics metrics;
    private final int maxCommandsPerPass;
    private final Clock clock;

    public UplinkPassService(TelecommandRepository telecommandRepository,
                             GroundStationService groundStationService,
                             UplinkChannel uplinkChannel,
                             AuditService auditService,
                             CommandMetrics metrics,
                             GroundSegmentProperties properties,
                             Clock clock) {
        this.telecommandRepository = telecommandRepository;
        this.groundStationService = groundStationService;
        this.uplinkChannel = uplinkChannel;
        this.auditService = auditService;
        this.metrics = metrics;
        this.maxCommandsPerPass = properties.uplink().maxCommandsPerPass();
        this.clock = clock;
    }

    @Transactional
    public PassReport executePass(UUID spacecraftId) {
        Instant now = clock.instant();
        Optional<ContactWindow> openWindow = groundStationService.openWindow(spacecraftId, now);
        if (openWindow.isEmpty()) {
            return PassReport.noContact(spacecraftId);
        }
        ContactWindow window = openWindow.get();
        List<Telecommand> dispatchable = telecommandRepository.findDispatchable(
                spacecraftId, CommandState.VALIDATED, now, PageRequest.of(0, maxCommandsPerPass));

        int uplinked = 0;
        int failed = 0;
        int deferred = 0;
        for (Telecommand command : dispatchable) {
            if (!window.reserve(command.getPayloadBytes())) {
                deferred++;
                continue;
            }
            command.schedule(window.getId(), now);
            auditService.record(AuditAction.COMMAND_SCHEDULED, "telecommand", command.getId().toString(),
                    Map.of("contactWindowId", window.getId().toString(),
                            "groundStation", window.getGroundStation().getCode()));

            UplinkChannel.UplinkResult result = uplinkChannel.transmit(command, window);
            if (result.transmitted()) {
                command.markUplinked(clock.instant());
                metrics.uplinked(command.getCriticality());
                auditService.record(AuditAction.COMMAND_UPLINKED, "telecommand", command.getId().toString(),
                        Map.of("groundStation", window.getGroundStation().getCode(),
                                "payloadBytes", Integer.toString(command.getPayloadBytes())));
                uplinked++;
            } else {
                command.fail(result.detail(), clock.instant());
                metrics.completed(CommandState.FAILED);
                auditService.record(AuditAction.COMMAND_FAILED, "telecommand", command.getId().toString(),
                        Map.of("reason", result.detail()));
                failed++;
            }
        }
        return new PassReport(spacecraftId, window.getId(), uplinked, failed, deferred);
    }

    public record PassReport(UUID spacecraftId, UUID contactWindowId, int uplinked, int failed, int deferred) {

        public static PassReport noContact(UUID spacecraftId) {
            return new PassReport(spacecraftId, null, 0, 0, 0);
        }

        public boolean hadContact() {
            return contactWindowId != null;
        }
    }
}
