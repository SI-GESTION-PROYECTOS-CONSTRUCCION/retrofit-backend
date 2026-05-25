package com.retrofit.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GanttItemResponseDto {
    private Long id;
    private String name;
    private Double totalQuantity;
    private Double laborYield;
    private String code;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long predecessorId;

    private Integer baseDurationDays;
    private Long parentId;
    private String type;
    private Double currentProgressPercentage;
}