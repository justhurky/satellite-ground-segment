package io.github.justhurky.groundsegment.command;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CommandMetrics {

    public static final String COMMANDS_SUBMITTED = "groundsegment.commands.submitted";
    public static final String COMMANDS_REJECTED = "groundsegment.commands.rejected";
    public static final String COMMANDS_UPLINKED = "groundsegment.commands.uplinked";
    public static final String COMMANDS_COMPLETED = "groundsegment.commands.completed";

    private final MeterRegistry meterRegistry;

    public CommandMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void submitted(CommandCriticality criticality) {
        meterRegistry.counter(COMMANDS_SUBMITTED, "criticality", criticality.name()).increment();
    }

    public void rejected(String reason) {
        meterRegistry.counter(COMMANDS_REJECTED, "reason", reason).increment();
    }

    public void uplinked(CommandCriticality criticality) {
        meterRegistry.counter(COMMANDS_UPLINKED, "criticality", criticality.name()).increment();
    }

    public void completed(CommandState state) {
        meterRegistry.counter(COMMANDS_COMPLETED, "state", state.name()).increment();
    }
}
