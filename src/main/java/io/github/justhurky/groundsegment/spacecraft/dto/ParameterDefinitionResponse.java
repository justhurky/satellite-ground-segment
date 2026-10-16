package io.github.justhurky.groundsegment.spacecraft.dto;

import io.github.justhurky.groundsegment.spacecraft.ParameterDefinition;

public record ParameterDefinitionResponse(
        long id,
        int parameterSlot,
        String name,
        String unit,
        Double redLow,
        Double yellowLow,
        Double yellowHigh,
        Double redHigh) {

    public static ParameterDefinitionResponse from(ParameterDefinition definition) {
        return new ParameterDefinitionResponse(
                definition.getId(),
                definition.getParameterSlot(),
                definition.getName(),
                definition.getUnit(),
                definition.getRedLow(),
                definition.getYellowLow(),
                definition.getYellowHigh(),
                definition.getRedHigh());
    }
}
