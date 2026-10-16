package io.github.justhurky.groundsegment.command;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum CommandState {

    RECEIVED,
    VALIDATED,
    SCHEDULED,
    UPLINKED,
    ACKNOWLEDGED,
    FAILED,
    REJECTED,
    CANCELLED;

    private static final Map<CommandState, Set<CommandState>> ALLOWED_TRANSITIONS;

    static {
        Map<CommandState, Set<CommandState>> transitions = new EnumMap<>(CommandState.class);
        transitions.put(RECEIVED, EnumSet.of(VALIDATED, REJECTED));
        transitions.put(VALIDATED, EnumSet.of(SCHEDULED, CANCELLED, FAILED));
        transitions.put(SCHEDULED, EnumSet.of(UPLINKED, CANCELLED, FAILED));
        transitions.put(UPLINKED, EnumSet.of(ACKNOWLEDGED, FAILED));
        transitions.put(ACKNOWLEDGED, EnumSet.noneOf(CommandState.class));
        transitions.put(FAILED, EnumSet.noneOf(CommandState.class));
        transitions.put(REJECTED, EnumSet.noneOf(CommandState.class));
        transitions.put(CANCELLED, EnumSet.noneOf(CommandState.class));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public boolean canTransitionTo(CommandState target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<CommandState> allowedTargets() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.get(this).isEmpty();
    }
}
