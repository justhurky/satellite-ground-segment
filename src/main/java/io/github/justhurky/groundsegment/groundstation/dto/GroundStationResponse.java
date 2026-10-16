package io.github.justhurky.groundsegment.groundstation.dto;

import io.github.justhurky.groundsegment.groundstation.GroundStation;
import java.util.UUID;

public record GroundStationResponse(
        UUID id,
        String code,
        String name,
        double latitudeDegrees,
        double longitudeDegrees,
        double altitudeMetres,
        boolean active) {

    public static GroundStationResponse from(GroundStation station) {
        return new GroundStationResponse(
                station.getId(),
                station.getCode(),
                station.getName(),
                station.getLatitudeDegrees(),
                station.getLongitudeDegrees(),
                station.getAltitudeMetres(),
                station.isActive());
    }
}
