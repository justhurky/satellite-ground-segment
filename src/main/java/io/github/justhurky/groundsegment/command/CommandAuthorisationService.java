package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.command.dto.SubmitCommandRequest;
import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import io.github.justhurky.groundsegment.identity.CurrentActor;
import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CommandAuthorisationService {

    private final CommandCatalogue commandCatalogue;
    private final CurrentActor currentActor;
    private final Duration maxClockSkew;

    public CommandAuthorisationService(CommandCatalogue commandCatalogue, CurrentActor currentActor,
                                       GroundSegmentProperties properties) {
        this.commandCatalogue = commandCatalogue;
        this.currentActor = currentActor;
        this.maxClockSkew = properties.uplink().maxClockSkew();
    }

    public CommandCatalogue.Rule authorise(Spacecraft spacecraft, SubmitCommandRequest request, Instant now) {
        CommandCatalogue.Rule rule = commandCatalogue.ruleFor(request.opcode())
                .orElseThrow(() -> new CommandNotPermittedException("Opcode "
                        + CommandCatalogue.normalise(request.opcode())
                        + " is not part of the command catalogue"));

        if (rule.criticality() != request.criticality()) {
            throw new CommandNotPermittedException("Opcode " + CommandCatalogue.normalise(request.opcode())
                    + " is classified " + rule.criticality() + " but was submitted as " + request.criticality());
        }
        if (!spacecraft.getMissionPhase().acceptsCommanding()) {
            throw new CommandNotPermittedException("Spacecraft " + spacecraft.getName()
                    + " is in mission phase " + spacecraft.getMissionPhase() + " and accepts no commands");
        }
        if (!rule.allowedPhases().contains(spacecraft.getMissionPhase())) {
            throw new CommandNotPermittedException("Opcode " + CommandCatalogue.normalise(request.opcode())
                    + " is not permitted in mission phase " + spacecraft.getMissionPhase());
        }
        if (!currentActor.hasRole(rule.requiredRole()) && !currentActor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Opcode " + CommandCatalogue.normalise(request.opcode())
                    + " requires role " + rule.requiredRole());
        }
        Duration skew = Duration.between(request.requestedAt(), now).abs();
        if (skew.compareTo(maxClockSkew) > 0) {
            throw new CommandStaleException("Command timestamp is " + skew.toSeconds()
                    + " seconds away from ground segment time, the accepted skew is "
                    + maxClockSkew.toSeconds() + " seconds");
        }
        if (request.executeAfter() != null && request.executeAfter().isBefore(request.requestedAt())) {
            throw new CommandNotPermittedException("executeAfter must not precede requestedAt");
        }
        return rule;
    }
}
