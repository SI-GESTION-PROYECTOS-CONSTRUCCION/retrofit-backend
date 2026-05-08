package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectRequestDto;
import com.retrofit.backend.dto.ProjectResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProjectService {
    Page<ProjectResponseDto> getAllProjects(Pageable pageable);
    ProjectResponseDto getProjectById(Long id);
    ProjectResponseDto createProject(ProjectRequestDto dto);
    ProjectResponseDto updateProject(Long id, ProjectRequestDto dto);
    void deleteProject(Long id);
}
