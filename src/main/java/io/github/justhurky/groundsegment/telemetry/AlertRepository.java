package io.github.justhurky.groundsegment.telemetry;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query("""
            select alert from Alert alert
            where alert.spacecraft.id = :spacecraftId
              and (:severity is null or alert.severity = :severity)
              and (:acknowledged is null or alert.acknowledged = :acknowledged)
            order by alert.raisedAt desc
            """)
    Page<Alert> search(@Param("spacecraftId") UUID spacecraftId,
                       @Param("severity") AlertSeverity severity,
                       @Param("acknowledged") Boolean acknowledged,
                       Pageable pageable);

    long countBySpacecraftIdAndAcknowledgedFalse(UUID spacecraftId);
}
