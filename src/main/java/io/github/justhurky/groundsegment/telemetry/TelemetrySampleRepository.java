package io.github.justhurky.groundsegment.telemetry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelemetrySampleRepository extends JpaRepository<TelemetrySample, Long> {

    @Query("""
            select sample from TelemetrySample sample
            where sample.frame.spacecraft.id = :spacecraftId
              and sample.parameterName = :parameterName
              and (:from is null or sample.generatedAt >= :from)
              and (:to is null or sample.generatedAt < :to)
            order by sample.generatedAt asc
            """)
    List<TelemetrySample> findSeries(@Param("spacecraftId") UUID spacecraftId,
                                     @Param("parameterName") String parameterName,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}
