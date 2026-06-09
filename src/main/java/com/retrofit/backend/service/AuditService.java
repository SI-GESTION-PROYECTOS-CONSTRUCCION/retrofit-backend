package com.retrofit.backend.service;

import com.retrofit.backend.dto.AuditLogResponseDto;
import com.retrofit.backend.dto.AuditStatsDto;
import org.springframework.data.domain.Page;

public interface AuditService {
    Page<AuditLogResponseDto> getAuditLogs(int page, int size, String search, String module, String action, String date);
    void logAction(String action, String module, Long recordId, Object oldData, Object newData);
    AuditStatsDto getAuditStats();
}
