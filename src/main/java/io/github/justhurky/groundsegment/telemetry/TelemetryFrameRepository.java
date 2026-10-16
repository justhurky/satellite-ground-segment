package io.github.justhurky.groundsegment.telemetry;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface TelemetryFrameRepository extends JpaRepository<TelemetryFrame, Long> {

    @Query("""
            select frame from TelemetryFrame frame
            where frame.spacecraft.id = :spacecraftId
              and (:apid is null or frame.apid = :apid)
              and (:from is null or frame.generatedAt >= :from)
              and (:to is null or frame.generatedAt < :to)
            """)
    Page<TelemetryFrame> search(@Param("spacecraftId") UUID spacecraftId,
                                @Param("apid") Integer apid,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);

    long countBySpacecraftId(UUID spacecraftId);
}
