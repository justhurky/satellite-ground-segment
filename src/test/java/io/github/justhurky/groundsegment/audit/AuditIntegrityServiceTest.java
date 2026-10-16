package io.github.justhurky.groundsegment.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.audit.dto.AuditIntegrityReport;
import io.github.justhurky.groundsegment.crypto.HashChain;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditIntegrityServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditIntegrityService auditIntegrityService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void startFromAnEmptyChain() {
        auditEventRepository.deleteAll();
        appendEvent("spacecraft-1", AuditAction.SPACECRAFT_REGISTERED);
        appendEvent("spacecraft-1", AuditAction.MISSION_PHASE_CHANGED);
        appendEvent("telecommand-1", AuditAction.COMMAND_SUBMITTED);
    }

    @Test
    void verifiesAChainItJustWrote() {
        AuditIntegrityReport report = auditIntegrityService.verify();

        assertThat(report.intact()).isTrue();
        assertThat(report.eventCount()).isEqualTo(3);
        assertThat(report.firstInvalidSequence()).isNull();
        assertThat(report.headHash()).isEqualTo(
                auditEventRepository.findTopByOrderByEventSequenceDesc().orElseThrow().getHash());
    }

    @Test
    void anchorsTheFirstEntryToTheGenesisHash() {
        assertThat(auditEventRepository.findAllByOrderByEventSequenceAsc().getFirst().getPreviousHash())
                .isEqualTo(HashChain.GENESIS_HASH);
    }

    @Test
    void linksEveryEntryToItsPredecessor() {
        var events = auditEventRepository.findAllByOrderByEventSequenceAsc();

        assertThat(events.get(1).getPreviousHash()).isEqualTo(events.get(0).getHash());
        assertThat(events.get(2).getPreviousHash()).isEqualTo(events.get(1).getHash());
    }

    @Test
    void detectsAnAlteredDetailField() {
        jdbcTemplate.update("update audit_event set details = ? where event_sequence = 2", "tampered=yes");

        AuditIntegrityReport report = auditIntegrityService.verify();

        assertThat(report.intact()).isFalse();
        assertThat(report.firstInvalidSequence()).isEqualTo(2L);
        assertThat(report.finding()).contains("Recomputed hash");
    }

    @Test
    void detectsAnAlteredActor() {
        jdbcTemplate.update("update audit_event set actor = ? where event_sequence = 3", "somebody-else");

        assertThat(auditIntegrityService.verify().firstInvalidSequence()).isEqualTo(3L);
    }

    @Test
    void detectsARewrittenLink() {
        jdbcTemplate.update("update audit_event set previous_hash = ? where event_sequence = 3",
                HashChain.GENESIS_HASH);

        AuditIntegrityReport report = auditIntegrityService.verify();

        assertThat(report.intact()).isFalse();
        assertThat(report.firstInvalidSequence()).isEqualTo(3L);
        assertThat(report.finding()).contains("previous hash");
    }

    @Test
    void detectsADeletedEntry() {
        jdbcTemplate.update("delete from audit_event where event_sequence = 2");

        AuditIntegrityReport report = auditIntegrityService.verify();

        assertThat(report.intact()).isFalse();
        assertThat(report.firstInvalidSequence()).isEqualTo(3L);
        assertThat(report.finding()).contains("Expected sequence 2");
    }

    @Test
    void treatsAnEmptyChainAsIntact() {
        auditEventRepository.deleteAll();

        AuditIntegrityReport report = auditIntegrityService.verify();

        assertThat(report.intact()).isTrue();
        assertThat(report.eventCount()).isZero();
        assertThat(report.headHash()).isEqualTo(HashChain.GENESIS_HASH);
    }

    @Test
    void recordsTheSuppliedActorWhenOneIsGiven() {
        AuditEvent event = auditService.recordAs("ground-station-kir", AuditAction.TELEMETRY_FRAME_ACCEPTED,
                "telemetry-frame", "41", Map.of("apid", "100"));

        assertThat(event.getActor()).isEqualTo("ground-station-kir");
        assertThat(event.getEventSequence()).isEqualTo(4L);
        assertThat(auditIntegrityService.verify().intact()).isTrue();
    }

    private void appendEvent(String resourceId, AuditAction action) {
        auditService.record(action, "spacecraft", resourceId, Map.of("reason", action.name()));
    }
}
