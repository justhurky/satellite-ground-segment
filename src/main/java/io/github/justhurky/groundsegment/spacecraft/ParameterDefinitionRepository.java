package io.github.justhurky.groundsegment.spacecraft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParameterDefinitionRepository extends JpaRepository<ParameterDefinition, Long> {

    List<ParameterDefinition> findAllBySpacecraftIdOrderByParameterSlotAsc(UUID spacecraftId);

    Optional<ParameterDefinition> findBySpacecraftIdAndParameterSlot(UUID spacecraftId, int parameterSlot);

    Optional<ParameterDefinition> findBySpacecraftIdAndName(UUID spacecraftId, String name);
}
