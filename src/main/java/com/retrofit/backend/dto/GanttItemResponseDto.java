package com.retrofit.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

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
    private List<GanttDependencyDto> dependencies;

    private Integer baseDurationDays;
    private Long parentId;
    private Integer level;
    private String type;
    private Double currentProgressPercentage;
}
