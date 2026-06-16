package com.retrofit.backend.controller;

import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/projects/{projectId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;
    private final AuditService auditService;

    @GetMapping("/apu")
    public ResponseEntity<byte[]> downloadApuReport(@PathVariable Long projectId) {
        try {
            byte[] pdfBytes = pdfReportService.generateApuReport(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_apu_proyecto_" + projectId + ".pdf");

            // Registrar en el log de auditoría la exportación del APU
            auditService.logAction("EXPORT", "Presupuestos", projectId, null, "Descarga de reporte APU en PDF");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/avances")
    public ResponseEntity<byte[]> downloadProgressReport(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String itemCode) {
        try {
            byte[] pdfBytes = pdfReportService.generateProgressReport(projectId, startDate, endDate, itemCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_avances_proyecto_" + projectId + ".pdf");

            auditService.logAction("EXPORT", "Reportes de Avance", projectId, null, "Descarga de reporte de avances en PDF");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/inventario")
    public ResponseEntity<byte[]> downloadInventoryReport(@PathVariable Long projectId) {
        try {
            byte[] pdfBytes = pdfReportService.generateInventoryReport(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_inventario_proyecto_" + projectId + ".pdf");

            auditService.logAction("EXPORT", "Inventario", projectId, null, "Descarga de reporte de inventario en PDF");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
