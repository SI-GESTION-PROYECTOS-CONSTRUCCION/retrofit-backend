package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditStatsDto {
    private long todayEvents;
    private long modifications;
    private long alerts;
}
