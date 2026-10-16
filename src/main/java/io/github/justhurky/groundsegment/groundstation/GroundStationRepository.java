package io.github.justhurky.groundsegment.groundstation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroundStationRepository extends JpaRepository<GroundStation, UUID> {

    Optional<GroundStation> findByCode(String code);

    boolean existsByCode(String code);
}
