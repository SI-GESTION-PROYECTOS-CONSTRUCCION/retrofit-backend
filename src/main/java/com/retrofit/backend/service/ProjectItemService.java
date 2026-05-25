package com.retrofit.backend.service;

import com.retrofit.backend.dto.*;

import java.util.List;

public interface ProjectItemService {
    List<ProjectItemResponseDto> getItemsByProjectId(Long projectId);
    List<ProjectItemResponseDto> saveBulkItems(Long projectId, List<ProjectItemRequestDto> dtos);
    ProjectItemResponseDto saveApuDetails(Long itemId, Double laborYield, Double equipmentYield, List<ProjectItemResourceRequestDto> dtos);
    ProjectItemResponseDto getProjectItemById(Long itemId);
    void updateGanttDates(Long itemId, GanttUpdateDto dto);
    List<GanttItemResponseDto> getGanttItems(Long projectId);
}