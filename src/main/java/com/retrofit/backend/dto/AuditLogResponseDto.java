package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponseDto {
    private String logId;
    private LocalDateTime timestamp;
    private String userName;
    private String userRole;
    private String action;
    private String module;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String oldData;
    private String newData;
}
