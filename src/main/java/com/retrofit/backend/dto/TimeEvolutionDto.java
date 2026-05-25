package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeEvolutionDto {
    private String dateLabel;
    private Double plannedValueAccumulated;
    private Double earnedValueAccumulated;
    private Double actualCostAccumulated;
}
