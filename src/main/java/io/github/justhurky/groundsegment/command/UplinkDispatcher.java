package io.github.justhurky.groundsegment.command;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UplinkDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UplinkDispatcher.class);

    private final TelecommandRepository telecommandRepository;
    private final UplinkPassService uplinkPassService;

    public UplinkDispatcher(TelecommandRepository telecommandRepository, UplinkPassService uplinkPassService) {
        this.telecommandRepository = telecommandRepository;
        this.uplinkPassService = uplinkPassService;
    }

    @Scheduled(fixedDelayString = "${ground-segment.uplink.dispatch-interval}")
    public void dispatchPendingCommands() {
        List<UUID> spacecraftIds = telecommandRepository.findSpacecraftIdsWithState(CommandState.VALIDATED);
        for (UUID spacecraftId : spacecraftIds) {
            try {
                UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraftId);
                if (report.hadContact()) {
                    log.info("Pass for spacecraft {} uplinked {}, failed {}, deferred {}",
                            spacecraftId, report.uplinked(), report.failed(), report.deferred());
                }
            } catch (RuntimeException e) {
                log.error("Uplink pass for spacecraft {} failed", spacecraftId, e);
            }
        }
    }
}
