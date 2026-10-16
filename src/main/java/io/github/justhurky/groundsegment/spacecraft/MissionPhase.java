package io.github.justhurky.groundsegment.spacecraft;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum MissionPhase {

    COMMISSIONING,
    NOMINAL,
    SAFE_MODE,
    DECOMMISSIONED;

    private static final Map<MissionPhase, Set<MissionPhase>> ALLOWED_TRANSITIONS;

    static {
        Map<MissionPhase, Set<MissionPhase>> transitions = new EnumMap<>(MissionPhase.class);
        transitions.put(COMMISSIONING, EnumSet.of(NOMINAL, SAFE_MODE, DECOMMISSIONED));
        transitions.put(NOMINAL, EnumSet.of(SAFE_MODE, DECOMMISSIONED));
        transitions.put(SAFE_MODE, EnumSet.of(NOMINAL, DECOMMISSIONED));
        transitions.put(DECOMMISSIONED, EnumSet.noneOf(MissionPhase.class));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public boolean canTransitionTo(MissionPhase target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<MissionPhase> allowedTargets() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean acceptsCommanding() {
        return this != DECOMMISSIONED;
    }
}
