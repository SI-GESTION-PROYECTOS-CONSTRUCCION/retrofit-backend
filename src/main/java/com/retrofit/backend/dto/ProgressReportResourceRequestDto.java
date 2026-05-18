package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class ProgressReportResourceRequestDto {
    private Long resourceId;
    private Double theoreticalQuantity;
    private Double realQuantity;
}