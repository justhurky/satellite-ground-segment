package io.github.justhurky.groundsegment.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelecommandRepository extends JpaRepository<Telecommand, UUID> {

    boolean existsBySpacecraftIdAndNonce(UUID spacecraftId, String nonce);

    @Query("""
            select command from Telecommand command
            where command.spacecraft.id = :spacecraftId
              and command.state = :state
              and (command.executeAfter is null or command.executeAfter <= :now)
            order by command.priority desc, command.submittedAt asc
            """)
    List<Telecommand> findDispatchable(@Param("spacecraftId") UUID spacecraftId,
                                       @Param("state") CommandState state,
                                       @Param("now") Instant now,
                                       Pageable pageable);

    @Query("""
            select command from Telecommand command
            where command.spacecraft.id = :spacecraftId
              and (:state is null or command.state = :state)
            order by command.submittedAt desc
            """)
    Page<Telecommand> search(@Param("spacecraftId") UUID spacecraftId,
                             @Param("state") CommandState state,
                             Pageable pageable);

    @Query("""
            select distinct command.spacecraft.id from Telecommand command
            where command.state = :state
            """)
    List<UUID> findSpacecraftIdsWithState(@Param("state") CommandState state);
}
