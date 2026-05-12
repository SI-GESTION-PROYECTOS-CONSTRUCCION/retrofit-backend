package com.retrofit.backend.controller;

import com.retrofit.backend.dto.GroupedProgressReportDto;
import com.retrofit.backend.dto.ProgressReportRequestDto;
import com.retrofit.backend.dto.ProgressReportResponseDto;
import com.retrofit.backend.service.ProgressReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/progress-reports")
@RequiredArgsConstructor
public class ProgressReportController {

    private final ProgressReportService progressReportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createReport(
            @RequestPart("reportData") @Valid ProgressReportRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        progressReportService.createReportWithPhotos(dto, files);

        return new ResponseEntity<>("Reporte de avance registrado correctamente", HttpStatus.CREATED);
    }


    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<GroupedProgressReportDto>> getReportsByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) String itemCode) {

        List<GroupedProgressReportDto> groupedReports = progressReportService.getFilteredAndGroupedReports(projectId, startDate, endDate, itemCode);
        return ResponseEntity.ok(groupedReports);
    }
}