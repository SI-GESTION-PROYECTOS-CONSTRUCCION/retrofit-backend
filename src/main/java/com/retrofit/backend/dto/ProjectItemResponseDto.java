package com.retrofit.backend.dto;

import lombok.Data;

@Data
public class ProjectItemResponseDto {
    private Long id;
    private String description;
    private String code;
    private String unit;
    private Double unitPrice;
    private Double totalQuantity;
    private Double executedQuantity;
    private Integer level;
    private Double laborYield;
    private Double equipmentYield;
}