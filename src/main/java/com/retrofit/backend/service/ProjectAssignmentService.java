package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectAssignmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectAssignmentService {
    ProjectAssignmentDTO assignWorker(ProjectAssignmentDTO dto);
    Page<ProjectAssignmentDTO> getWorkersByProject(Long projectId, String search, Pageable pageable);
    List<ProjectAssignmentDTO> getActiveAssignments();
    void releaseWorker(Long assignmentId); // Para marcar como inactivo (salida de la obra)
}
