package io.github.justhurky.groundsegment.audit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Optional<AuditEvent> findTopByOrderByEventSequenceDesc();

    List<AuditEvent> findAllByOrderByEventSequenceAsc();

    Page<AuditEvent> findAllByOrderByEventSequenceDesc(Pageable pageable);

    Page<AuditEvent> findAllByActionOrderByEventSequenceDesc(AuditAction action, Pageable pageable);
}
