package com.retrofit.backend.controller;

import com.retrofit.backend.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;

    @GetMapping("/apu")
    public ResponseEntity<byte[]> downloadApuReport(@PathVariable Long projectId) {
        try {
            byte[] pdfBytes = pdfReportService.generateApuReport(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_apu_proyecto_" + projectId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
