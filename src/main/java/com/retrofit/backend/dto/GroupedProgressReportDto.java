package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupedProgressReportDto {
    private String period; // Ej: "MAYO 2026"
    private List<ProgressReportResponseDto> reports;
}