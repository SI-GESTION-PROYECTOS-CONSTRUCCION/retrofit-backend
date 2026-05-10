package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectAssignmentDTO;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectAssignment;
import com.retrofit.backend.model.Worker;
import com.retrofit.backend.repository.ProjectAssignmentRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.WorkerRepository;
import com.retrofit.backend.service.ProjectAssignmentService;
import lombok.RequiredArgsConstructor;
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

    @Override
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
    public List<ProjectAssignmentDTO> getWorkersByProject(Long projectId) {
        return assignmentRepository.findByProjectIdAndActiveTrue(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void releaseWorker(Long assignmentId) {
        ProjectAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    private ProjectAssignmentDTO mapToDTO(ProjectAssignment assignment) {
        return ProjectAssignmentDTO.builder()
                .id(assignment.getId())
                .projectId(assignment.getProject().getId())
                .projectName(assignment.getProject().getName())
                .workerId(assignment.getWorker().getId())
                .workerName(assignment.getWorker().getUser() != null ?
                        assignment.getWorker().getUser().getName() + " " + assignment.getWorker().getUser().getLastName() :
                        "Personal externo")
                .position(assignment.getWorker().getPosition())
                .assignedAt(assignment.getAssignedAt())
                .active(assignment.isActive())
                .build();
    }
}
