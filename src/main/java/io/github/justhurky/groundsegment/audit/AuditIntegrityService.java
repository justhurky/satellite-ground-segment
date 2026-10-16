package io.github.justhurky.groundsegment.audit;

import io.github.justhurky.groundsegment.audit.dto.AuditIntegrityReport;
import io.github.justhurky.groundsegment.crypto.HashChain;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditIntegrityService {

    private final AuditEventRepository auditEventRepository;
    private final HashChain hashChain;

    public AuditIntegrityService(AuditEventRepository auditEventRepository, HashChain hashChain) {
        this.auditEventRepository = auditEventRepository;
        this.hashChain = hashChain;
    }

    @Transactional(readOnly = true)
    public AuditIntegrityReport verify() {
        List<AuditEvent> events = auditEventRepository.findAllByOrderByEventSequenceAsc();
        String expectedPreviousHash = HashChain.GENESIS_HASH;
        long expectedSequence = 1L;
        for (AuditEvent event : events) {
            if (event.getEventSequence() != expectedSequence) {
                return AuditIntegrityReport.broken(events.size(), event.getEventSequence(),
                        "Expected sequence " + expectedSequence + " but found " + event.getEventSequence());
            }
            if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                return AuditIntegrityReport.broken(events.size(), event.getEventSequence(),
                        "Recorded previous hash does not match the preceding event");
            }
            String recomputed = hashChain.link(event.getPreviousHash(), event.canonicalRecord());
            if (!recomputed.equals(event.getHash())) {
                return AuditIntegrityReport.broken(events.size(), event.getEventSequence(),
                        "Recomputed hash does not match the stored hash");
            }
            expectedPreviousHash = event.getHash();
            expectedSequence++;
        }
        return AuditIntegrityReport.intact(events.size(), expectedPreviousHash);
    }
}
