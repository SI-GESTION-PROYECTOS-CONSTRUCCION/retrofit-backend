package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LatestProgressDto {
    private Long reportId;
    private String date;
    private String itemCode;
    private String itemDescription;
    private Double executedQuantity;
    private String unit;
}
