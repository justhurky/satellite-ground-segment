package io.github.justhurky.groundsegment.spacecraft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpacecraftRepository extends JpaRepository<Spacecraft, UUID> {

    Optional<Spacecraft> findByName(String name);

    Optional<Spacecraft> findByNoradId(int noradId);

    boolean existsByName(String name);

    boolean existsByNoradId(int noradId);

    List<Spacecraft> findAllByMissionPhaseIn(List<MissionPhase> phases);
}
