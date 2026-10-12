package com.retrofit.backend.controller;

import com.retrofit.backend.dto.AuditLogResponseDto;
import com.retrofit.backend.dto.AuditStatsDto;
import com.retrofit.backend.model.AuditLog;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;
    private final PdfReportService pdfReportService;

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

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<byte[]> exportAuditReport(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String requestedBy = userDetails != null ? userDetails.getUsername() : "Administrador";
            List<AuditLog> logs = auditService.getAuditLogsForReport(search, module, action, date, startDate, endDate);

            LocalDate effectiveStartDate = startDate != null ? startDate : (date != null && !date.isBlank() ? LocalDate.parse(date.trim()) : null);
            LocalDate effectiveEndDate = endDate != null ? endDate : (date != null && !date.isBlank() ? LocalDate.parse(date.trim()) : null);

            byte[] pdfBytes = pdfReportService.generateAuditReport(
                    logs,
                    search,
                    module,
                    action,
                    effectiveStartDate,
                    effectiveEndDate,
                    requestedBy);

            auditService.logAction("EXPORT", "Auditoría", null, null, "Generación y descarga de reporte oficial de bitácoras en PDF");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_auditoria_" + LocalDate.now() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error al exportar reporte de auditoría en PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
