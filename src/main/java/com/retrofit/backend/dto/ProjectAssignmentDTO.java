package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectAssignmentDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long workerId;
    private String workerName;
    private String position;
    private Timestamp assignedAt;
    private Boolean active;
}
