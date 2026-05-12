package com.retrofit.backend.service;

import com.retrofit.backend.dto.GroupedProgressReportDto;
import com.retrofit.backend.dto.ProgressReportRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface ProgressReportService {
    void createReportWithPhotos(ProgressReportRequestDto dto, List<MultipartFile> files);
    List<GroupedProgressReportDto> getFilteredAndGroupedReports(Long projectId, LocalDate startDate, LocalDate endDate, String itemCode);
}
