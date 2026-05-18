package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class ProgressReportResourceResponseDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private String resourceUnit;
    private String resourceType;
    private Double theoreticalQuantity;
    private Double realQuantity;
}