package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectRequestDto;
import com.retrofit.backend.dto.ProjectResponseDto;
import com.retrofit.backend.enums.ProjectPriority;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project mockProject;
    private ProjectRequestDto requestDto;

    @BeforeEach
    void setUp() {
        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setCode("PRJ-001");
        mockProject.setName("Test Project");
        mockProject.setClient("Test Client");
        mockProject.setLocation("Lima, Peru");
        mockProject.setDescription("Description");
        mockProject.setStartDate(LocalDate.now());
        mockProject.setStatus(ProjectStatus.PLANNING);
        mockProject.setPriority(ProjectPriority.HIGH);

        requestDto = new ProjectRequestDto();
        requestDto.setCode("PRJ-001");
        requestDto.setName("Test Project");
        requestDto.setClient("Test Client");
        requestDto.setLocation("Lima, Peru");
        requestDto.setDescription("Description");
        requestDto.setStartDate(LocalDate.now());
        requestDto.setStatus("PLANNING");
        requestDto.setPriority("HIGH");
    }

    @Test
    void testGetAllProjects() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(Collections.singletonList(mockProject));
        
        when(projectRepository.findWithFilters("", null, null, pageable)).thenReturn(page);

        Page<ProjectResponseDto> result = projectService.getAllProjects("", null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PRJ-001", result.getContent().get(0).getCode());
        verify(projectRepository, times(1)).findWithFilters("", null, null, pageable);
    }

    @Test
    void testGetProjectById_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));

        ProjectResponseDto result = projectService.getProjectById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PRJ-001", result.getCode());
    }

    @Test
    void testGetProjectById_NotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(1L));
    }

    @Test
    void testCreateProject_Success() {
        when(projectRepository.existsByCode(requestDto.getCode())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        ProjectResponseDto result = projectService.createProject(requestDto);

        assertNotNull(result);
        assertEquals("PRJ-001", result.getCode());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testCreateProject_DuplicateCode() {
        when(projectRepository.existsByCode(requestDto.getCode())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.createProject(requestDto);
        });

        assertEquals("Project code already exists", exception.getMessage());
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void testCreateProject_WithManagerSuccess() {
        requestDto.setManagerId(10L);
        User manager = new User();
        manager.setId(10L);
        manager.setName("John");
        manager.setLastName("Doe");

        when(projectRepository.existsByCode(requestDto.getCode())).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        
        mockProject.setManager(manager);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        ProjectResponseDto result = projectService.createProject(requestDto);

        assertNotNull(result);
        assertEquals(10L, result.getManagerId());
        assertEquals("John Doe", result.getManagerFullName());
        verify(userRepository, times(1)).findById(10L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }
}
