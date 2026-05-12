package com.retrofit.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProgressReportResponseDto {
    private Long id;
    private String itemCode;
    private String itemDescription;
    private LocalDate reportDate;
    private Double executedQuantity;
    private String unit;
    private String observations;
    private List<String> photoUrls;
}