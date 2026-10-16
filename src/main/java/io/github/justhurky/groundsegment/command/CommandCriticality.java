package io.github.justhurky.groundsegment.command;

public enum CommandCriticality {

    ROUTINE(0),
    SENSITIVE(1),
    CRITICAL(2);

    private final int priority;

    CommandCriticality(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
