package com.retrofit.backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GanttReportItemDto {
    private String code;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private Double progress;
    private Integer level;
    private String paddingLeft;
    private String barStyle;
    private boolean parent;
    private double offsetPercent;
    private double widthPercent;
}
