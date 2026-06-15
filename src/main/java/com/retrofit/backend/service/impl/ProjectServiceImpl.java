package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.ProjectRequestDto;
import com.retrofit.backend.dto.ProjectResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.enums.ProjectPriority;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public Page<ProjectResponseDto> getAllProjects(String search, String priorityStr, String statusStr, Pageable pageable) {

        ProjectPriority priority = null;
        if (priorityStr != null && !priorityStr.trim().isEmpty()) {
            try {
                priority = ProjectPriority.valueOf(priorityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignorado
            }
        }

        ProjectStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = ProjectStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignorado
            }
        }


        String finalSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : "";

        return projectRepository.findWithFilters(finalSearch, priority, status, pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Proyectos")
    public ProjectResponseDto createProject(ProjectRequestDto dto) {
        if(projectRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Project code already exists");
        }
        Project project = new Project();
        return saveProjectFromDto(project, dto);
    }

    @Override
    @Transactional
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto dto) {
        // 1. Buscamos el proyecto a actualizar
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectResponseDto estadoAnterior = convertToDto(project);

        // 2. Validamos que si el usuario cambió el código, el nuevo código no le pertenezca a OTRO proyecto
        projectRepository.findByCode(dto.getCode())
                .ifPresent(existingProject -> {
                    if (!existingProject.getId().equals(id)) {
                        // Usamos exactamente el mismo mensaje que en crear,
                        // para que el GlobalExceptionHandler pinte el error en el frontend de forma mágica.
                        throw new IllegalArgumentException("Project code already exists");
                    }
                });

        // 3. Guardamos los datos primero para obtener el estado nuevo real
        ProjectResponseDto estadoNuevo = saveProjectFromDto(project, dto);
        auditService.logAction("UPDATE", "Proyectos", project.getId(), estadoAnterior, estadoNuevo);
        return estadoNuevo;
    }

    @Override
    @Transactional
    @AuditChange(action = "DELETE", module = "Proyectos")
    public void deleteProject(Long id) {
        if(!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found");
        }
        projectRepository.deleteById(id);
    }

    @Override
    public ProjectResponseDto getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    @Override
    public ProjectResponseDto getProjectByCode(String code) {
        return projectRepository.findByCode(code)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectResponseDto saveProjectFromDto(Project project, ProjectRequestDto dto) {
        project.setCode(dto.getCode());
        project.setName(dto.getName());
        project.setClient(dto.getClient());
        project.setLocation(dto.getLocation());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());

        try {
            project.setStatus(ProjectStatus.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de proyecto inválido: " + dto.getStatus());
        }

        try {
            project.setPriority(ProjectPriority.valueOf(dto.getPriority().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Prioridad de proyecto inválida: " + dto.getPriority());
        }

        if(dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró al responsable con el ID proporcionado"));
            project.setManager(manager);
        }

        return convertToDto(projectRepository.save(project));
    }

    private ProjectResponseDto convertToDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();
        dto.setId(project.getId());
        dto.setCode(project.getCode());
        dto.setName(project.getName());
        dto.setClient(project.getClient());

        dto.setLocation(project.getLocation());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());

        dto.setStatus(project.getStatus().name());
        dto.setPriority(project.getPriority().name());
        dto.setCurrentProgress(project.getCurrentProgress());
        dto.setTotalBudget(project.getTotalBudget());
        if(project.getManager() != null) {
            dto.setManagerId(project.getManager().getId());
            dto.setManagerFullName(project.getManager().getName() + " " + project.getManager().getLastName());
        }
        return dto;
    }
}
