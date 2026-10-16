package io.github.justhurky.groundsegment.identity;

public enum Role {

    ADMIN,
    FLIGHT_DIRECTOR,
    OPERATOR,
    ANALYST,
    AUDITOR,
    GROUND_STATION;

    public static final String AUTHORITY_PREFIX = "ROLE_";

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }
}
