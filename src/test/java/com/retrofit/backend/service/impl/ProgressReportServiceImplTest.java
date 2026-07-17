package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProgressReportRequestDto;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.model.ProgressReport;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.repository.ProgressPhotoRepository;
import com.retrofit.backend.repository.ProgressReportRepository;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.ResourceRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProgressReportServiceImplTest {

    @Mock
    private ProjectItemRepository itemRepository;

    @Mock
    private ProgressReportRepository reportRepository;

    @Mock
    private ProgressPhotoRepository photoRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProgressReportServiceImpl progressReportService;

    private ProjectItem mockItem;
    private Project mockProject;
    private ProgressReportRequestDto requestDto;

    @BeforeEach
    void setUp() {
        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setStatus(ProjectStatus.IN_PROGRESS);
        mockProject.setCurrentProgress(50.0);
        mockProject.setStartDate(LocalDate.now().minusDays(10));

        mockItem = new ProjectItem();
        mockItem.setId(100L);
        mockItem.setProject(mockProject);
        mockItem.setTotalQuantity(100.0);
        
        mockProject.setItems(Collections.singletonList(mockItem));

        requestDto = new ProgressReportRequestDto();
        requestDto.setProjectItemId(100L);
        requestDto.setReportDate(LocalDate.now());
        requestDto.setExecutedQuantity(20.0);
        requestDto.setObservations("Test Observation");
    }

    @Test
    void testCreateReportWithPhotos_Success() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(mockItem));
        when(reportRepository.sumExecutedQuantityByItemId(100L)).thenReturn(50.0); // Remaining is 50.0

        ProgressReport savedReport = new ProgressReport();
        savedReport.setId(10L);
        savedReport.setProjectItem(mockItem);
        when(reportRepository.save(any(ProgressReport.class))).thenReturn(savedReport);

        assertDoesNotThrow(() -> {
            progressReportService.createReportWithPhotos(requestDto, Collections.emptyList());
        });

        verify(reportRepository, times(1)).save(any(ProgressReport.class));
        verify(projectRepository, times(1)).save(mockProject);
    }

    @Test
    void testCreateReportWithPhotos_ExceedsQuantity() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(mockItem));
        when(reportRepository.sumExecutedQuantityByItemId(100L)).thenReturn(90.0); // Remaining is 10.0

        // Trying to report 20.0
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            progressReportService.createReportWithPhotos(requestDto, Collections.emptyList());
        });

        assertTrue(exception.getMessage().contains("excede el saldo disponible de la partida"));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }

    @Test
    void testCreateReportWithPhotos_ProjectCompleted() {
        mockProject.setStatus(ProjectStatus.COMPLETED);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(mockItem));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            progressReportService.createReportWithPhotos(requestDto, Collections.emptyList());
        });

        assertTrue(exception.getMessage().contains("Este proyecto ya ha sido completado"));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }

    @Test
    void testCreateReportWithPhotos_BeforeStartDate() {
        requestDto.setReportDate(LocalDate.now().minusDays(15));
        when(itemRepository.findById(100L)).thenReturn(Optional.of(mockItem));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            progressReportService.createReportWithPhotos(requestDto, Collections.emptyList());
        });

        assertTrue(exception.getMessage().contains("no puede ser anterior a la fecha de inicio del proyecto"));
        verify(reportRepository, never()).save(any(ProgressReport.class));
    }
}
