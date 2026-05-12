package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectItemRequestDto;
import com.retrofit.backend.dto.ProjectItemResponseDto;
import java.util.List;

public interface ProjectItemService {
    List<ProjectItemResponseDto> getItemsByProjectId(Long projectId);
    List<ProjectItemResponseDto> saveBulkItems(Long projectId, List<ProjectItemRequestDto> dtos);
}