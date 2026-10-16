package io.github.justhurky.groundsegment.audit.dto;

public record AuditIntegrityReport(
        int eventCount,
        boolean intact,
        Long firstInvalidSequence,
        String headHash,
        String finding) {

    public static AuditIntegrityReport intact(int eventCount, String headHash) {
        return new AuditIntegrityReport(eventCount, true, null, headHash, "Audit chain verified");
    }

    public static AuditIntegrityReport broken(int eventCount, long firstInvalidSequence, String finding) {
        return new AuditIntegrityReport(eventCount, false, firstInvalidSequence, null, finding);
    }
}
