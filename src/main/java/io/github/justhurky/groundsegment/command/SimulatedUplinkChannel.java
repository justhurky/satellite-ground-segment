package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.groundstation.ContactWindow;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedUplinkChannel implements UplinkChannel {

    private static final Logger log = LoggerFactory.getLogger(SimulatedUplinkChannel.class);

    private final Clock clock;

    public SimulatedUplinkChannel(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UplinkResult transmit(Telecommand command, ContactWindow window) {
        if (!window.isOpenAt(clock.instant())) {
            return UplinkResult.failed("contact-window-closed");
        }
        if (command.getPayloadBytes() > window.getUplinkBudgetBytes()) {
            return UplinkResult.failed("payload-exceeds-window-budget");
        }
        log.info("Uplinked command {} ({}) to {} through {}", command.getId(), command.getOpcode(),
                command.getSpacecraft().getName(), window.getGroundStation().getCode());
        return UplinkResult.transmitted("simulated-uplink");
    }
}
