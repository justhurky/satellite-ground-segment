package io.github.justhurky.groundsegment.audit;

import io.github.justhurky.groundsegment.common.security.ActorProvider;
import io.github.justhurky.groundsegment.crypto.HashChain;
import java.time.Clock;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final HashChain hashChain;
    private final ActorProvider actorProvider;
    private final Clock clock;
    private final Object appendLock = new Object();

    public AuditService(AuditEventRepository auditEventRepository, HashChain hashChain,
                        ActorProvider actorProvider, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.hashChain = hashChain;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    @Transactional
    public AuditEvent record(AuditAction action, String resourceType, String resourceId,
                             Map<String, String> details) {
        return recordAs(actorProvider.actorName(), action, resourceType, resourceId, details);
    }

    @Transactional
    public AuditEvent recordAs(String actor, AuditAction action, String resourceType, String resourceId,
                               Map<String, String> details) {
        synchronized (appendLock) {
            AuditEvent previous = auditEventRepository.findTopByOrderByEventSequenceDesc().orElse(null);
            long nextSequence = previous == null ? 1L : previous.getEventSequence() + 1L;
            String previousHash = previous == null ? HashChain.GENESIS_HASH : previous.getHash();
            AuditEvent event = new AuditEvent(nextSequence, clock.instant(), actor, action,
                    resourceType, resourceId, details, previousHash);
            event.sealWith(hashChain.link(previousHash, event.canonicalRecord()));
            return auditEventRepository.saveAndFlush(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent recordIndependently(AuditAction action, String resourceType, String resourceId,
                                          Map<String, String> details) {
        return recordAs(actorProvider.actorName(), action, resourceType, resourceId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> find(AuditAction action, Pageable pageable) {
        if (action == null) {
            return auditEventRepository.findAllByOrderByEventSequenceDesc(pageable);
        }
        return auditEventRepository.findAllByActionOrderByEventSequenceDesc(action, pageable);
    }
}
