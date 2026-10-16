package io.github.justhurky.groundsegment.audit;

import io.github.justhurky.groundsegment.audit.dto.AuditEventResponse;
import io.github.justhurky.groundsegment.audit.dto.AuditIntegrityReport;
import io.github.justhurky.groundsegment.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@Validated
@PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
public class AuditController {

    private final AuditService auditService;
    private final AuditIntegrityService auditIntegrityService;

    public AuditController(AuditService auditService, AuditIntegrityService auditIntegrityService) {
        this.auditService = auditService;
        this.auditIntegrityService = auditIntegrityService;
    }

    @GetMapping
    public PageResponse<AuditEventResponse> events(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return PageResponse.of(auditService.find(action, PageRequest.of(page, size)), AuditEventResponse::from);
    }

    @GetMapping("/integrity")
    public AuditIntegrityReport integrity() {
        return auditIntegrityService.verify();
    }
}
