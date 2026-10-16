package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.telemetry.CommandAcknowledgementReceived;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CommandAcknowledgementListener {

    private final TelecommandService telecommandService;

    public CommandAcknowledgementListener(TelecommandService telecommandService) {
        this.telecommandService = telecommandService;
    }

    @EventListener
    public void onAcknowledgement(CommandAcknowledgementReceived acknowledgement) {
        telecommandService.applyAcknowledgement(acknowledgement);
    }
}
