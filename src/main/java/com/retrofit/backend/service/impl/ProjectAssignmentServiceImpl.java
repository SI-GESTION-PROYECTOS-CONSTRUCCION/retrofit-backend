package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectAssignmentDTO;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectAssignment;
import com.retrofit.backend.model.Worker;
import com.retrofit.backend.repository.ProjectAssignmentRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.repository.WorkerRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.ProjectAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectAssignmentServiceImpl implements ProjectAssignmentService {
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final WorkerRepository workerRepository;
    private final AuditService auditService;

    @Override
    @AuditChange(action = "CREATE", module = "Asignaciones")
    public ProjectAssignmentDTO assignWorker(ProjectAssignmentDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        Worker worker = workerRepository.findById(dto.getWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado"));

        ProjectAssignment assignment = ProjectAssignment.builder()
                .project(project)
                .worker(worker)
                .assignedAt(Timestamp.valueOf(LocalDateTime.now()))
                .active(true)
                .build();

        return mapToDTO(assignmentRepository.save(assignment));
    }

    @Override
    public Page<ProjectAssignmentDTO> getWorkersByProject(Long projectId, String search, Pageable pageable) {
        String finalSearch = (search == null) ? "" : search.trim();
        return assignmentRepository.findActiveAssignments(projectId, finalSearch, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<ProjectAssignmentDTO> getActiveAssignments() {
        return assignmentRepository.findByActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void releaseWorker(Long assignmentId) {
        ProjectAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        ProjectAssignmentDTO estadoAnterior = mapToDTO(assignment);
        assignment.setActive(false);
        ProjectAssignment saved = assignmentRepository.save(assignment);
        ProjectAssignmentDTO estadoNuevo = mapToDTO(saved);

        auditService.logAction("DELETE", "Asignaciones", assignmentId, estadoAnterior, estadoNuevo);
    }

    private ProjectAssignmentDTO mapToDTO(ProjectAssignment assignment) {
        return ProjectAssignmentDTO.builder()
                .id(assignment.getId())
                .projectId(assignment.getProject().getId())
                .projectName(assignment.getProject().getName())
                .workerId(assignment.getWorker().getId())
                .workerName(assignment.getWorker().getName() + " " + assignment.getWorker().getLastName())
                .position(assignment.getWorker().getPosition())
                .assignedAt(assignment.getAssignedAt())
                .active(assignment.isActive())
                .build();
    }
}
