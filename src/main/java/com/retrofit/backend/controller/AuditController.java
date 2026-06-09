package com.retrofit.backend.controller;

import com.retrofit.backend.dto.AuditLogResponseDto;
import com.retrofit.backend.dto.AuditStatsDto;
import com.retrofit.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponseDto>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(auditService.getAuditLogs(page, size, search, module, action, date));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<AuditStatsDto> getStats() {
        return ResponseEntity.ok(auditService.getAuditStats());
    }
}
