package com.retrofit.backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectResponseDto {
    private Long id;
    private String code;
    private String name;
    private String client;
    private String location;
    private String description;
    private LocalDate startDate;
    private String status;
    private String priority;
    private Double currentProgress;
    private Long managerId;
    private String managerFullName;
    private Double totalBudget;
}
