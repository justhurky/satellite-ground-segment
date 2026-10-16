package io.github.justhurky.groundsegment.groundstation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactWindowRepository extends JpaRepository<ContactWindow, UUID> {

    @Query("""
            select window from ContactWindow window
            where window.spacecraft.id = :spacecraftId
              and window.groundStation.active = true
              and window.startsAt <= :instant
              and window.endsAt > :instant
            order by window.endsAt asc
            """)
    List<ContactWindow> findOpenWindows(@Param("spacecraftId") UUID spacecraftId, @Param("instant") Instant instant);

    @Query("""
            select count(window) > 0 from ContactWindow window
            where window.spacecraft.id = :spacecraftId
              and window.groundStation.id = :groundStationId
              and window.startsAt < :endsAt
              and window.endsAt > :startsAt
            """)
    boolean overlaps(@Param("spacecraftId") UUID spacecraftId,
                     @Param("groundStationId") UUID groundStationId,
                     @Param("startsAt") Instant startsAt,
                     @Param("endsAt") Instant endsAt);

    List<ContactWindow> findAllBySpacecraftIdOrderByStartsAtAsc(UUID spacecraftId);

    Optional<ContactWindow> findFirstBySpacecraftIdAndEndsAtAfterOrderByStartsAtAsc(UUID spacecraftId, Instant after);
}
