package io.github.justhurky.groundsegment.groundstation.dto;

import io.github.justhurky.groundsegment.groundstation.ContactWindow;
import java.time.Instant;
import java.util.UUID;

public record ContactWindowResponse(
        UUID id,
        UUID spacecraftId,
        String groundStationCode,
        Instant startsAt,
        Instant endsAt,
        int uplinkBudgetBytes,
        int uplinkedBytes) {

    public static ContactWindowResponse from(ContactWindow window) {
        return new ContactWindowResponse(
                window.getId(),
                window.getSpacecraft().getId(),
                window.getGroundStation().getCode(),
                window.getStartsAt(),
                window.getEndsAt(),
                window.getUplinkBudgetBytes(),
                window.getUplinkedBytes());
    }
}
