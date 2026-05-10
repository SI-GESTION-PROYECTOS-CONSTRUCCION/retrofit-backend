package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectAssignmentDTO;

import java.util.List;

public interface ProjectAssignmentService {
    ProjectAssignmentDTO assignWorker(ProjectAssignmentDTO dto);
    List<ProjectAssignmentDTO> getWorkersByProject(Long projectId);
    void releaseWorker(Long assignmentId); // Para marcar como inactivo (salida de la obra)
}
