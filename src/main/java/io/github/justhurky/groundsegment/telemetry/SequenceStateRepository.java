package io.github.justhurky.groundsegment.telemetry;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SequenceStateRepository extends JpaRepository<SequenceState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SequenceState> findBySpacecraftIdAndApid(UUID spacecraftId, int apid);
}
