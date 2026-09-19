package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectDashboardResponseDto;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.repository.ProgressReportRepository;
import com.retrofit.backend.repository.ProgressReportResourceRepository;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectItemRepository projectItemRepository;

    @Mock
    private ProgressReportRepository progressReportRepository;

    @Mock
    private ProgressReportResourceRepository progressReportResourceRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setCode("PRJ-001");
        mockProject.setName("Test Project");
    }

    @Test
    void testGetProjectDashboard_CleanProjectZeroCosts() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
        when(projectItemRepository.calculatePlannedValueByProjectId(1L, null, null)).thenReturn(10000.0);
        when(progressReportRepository.calculateEarnedValueByProjectId(1L, null, null)).thenReturn(0.0);
        when(progressReportResourceRepository.calculateActualCostByProjectId(1L, null, null)).thenReturn(0.0);
        when(projectItemRepository.calculateTotalPlannedQuantityByProjectId(1L, null, null)).thenReturn(100.0);
        when(progressReportRepository.calculateTotalExecutedQuantityByProjectId(1L, null, null)).thenReturn(0.0);

        ProjectDashboardResponseDto result = dashboardService.getProjectDashboard(1L, null);

        assertNotNull(result);
        assertEquals(1L, result.getProjectId());
        assertEquals(10000.0, result.getPlannedValue());
        assertEquals(0.0, result.getEarnedValue());
        assertEquals(0.0, result.getActualCost());
        assertEquals(0.0, result.getPorcentajeAvance());
        assertTrue(result.getCriticalItems().isEmpty());
        assertTrue(result.getTimeEvolution().isEmpty());
        assertTrue(result.getRecentProgresses().isEmpty());

        // Verificamos que NUNCA se llamen las consultas masivas o pesadas
        verify(progressReportRepository, never()).sumEarnedValueByProjectIdGroupedByItemId(anyLong());
        verify(progressReportResourceRepository, never()).sumActualCostByProjectIdGroupedByItemId(anyLong());
        verify(progressReportResourceRepository, never()).calculateActualCostByResourceType(anyLong(), any(), any());
        verify(progressReportRepository, never()).getEarnedValueByDate(anyLong(), any(), any());
        verify(progressReportResourceRepository, never()).getActualCostByDate(anyLong(), any(), any());
    }

    @Test
    void testGetProjectDashboard_WithActualCostsAndCriticalItems() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
        when(projectItemRepository.calculatePlannedValueByProjectId(1L, null, null)).thenReturn(10000.0);
        when(progressReportRepository.calculateEarnedValueByProjectId(1L, null, null)).thenReturn(2000.0);
        when(progressReportResourceRepository.calculateActualCostByProjectId(1L, null, null)).thenReturn(3500.0);
        when(projectItemRepository.calculateTotalPlannedQuantityByProjectId(1L, null, null)).thenReturn(100.0);
        when(progressReportRepository.calculateTotalExecutedQuantityByProjectId(1L, null, null)).thenReturn(20.0);

        ProjectItem item1 = new ProjectItem();
        item1.setId(10L);
        item1.setCode("01.01");
        item1.setDescription("Excavación");

        ProjectItem item2 = new ProjectItem();
        item2.setId(20L);
        item2.setCode("01.02");
        item2.setDescription("Concreto");

        when(projectItemRepository.findByProjectId(1L)).thenReturn(List.of(item1, item2));

        // EV agrupado: item1 = 1000.0, item2 = 1000.0
        List<Object[]> evRows = new ArrayList<>();
        evRows.add(new Object[]{10L, 1000.0});
        evRows.add(new Object[]{20L, 1000.0});
        when(progressReportRepository.sumEarnedValueByProjectIdGroupedByItemId(1L)).thenReturn(evRows);

        // AC agrupado: item1 tiene sobrecosto (2500.0 > 1000.0), item2 está en costo (1000.0 == 1000.0)
        List<Object[]> acRows = new ArrayList<>();
        acRows.add(new Object[]{10L, 2500.0});
        acRows.add(new Object[]{20L, 1000.0});
        when(progressReportResourceRepository.sumActualCostByProjectIdGroupedByItemId(1L)).thenReturn(acRows);

        when(progressReportResourceRepository.calculateActualCostByResourceType(1L, null, null)).thenReturn(new ArrayList<>());
        when(progressReportRepository.getEarnedValueByDate(1L, null, null)).thenReturn(new ArrayList<>());
        when(progressReportResourceRepository.getActualCostByDate(1L, null, null)).thenReturn(new ArrayList<>());
        when(progressReportRepository.findRecentReports(eq(1L), any(), any(), any())).thenReturn(new ArrayList<>());

        ProjectDashboardResponseDto result = dashboardService.getProjectDashboard(1L, null);

        assertNotNull(result);
        assertEquals(3500.0, result.getActualCost());
        assertEquals(2000.0, result.getEarnedValue());
        assertEquals(1, result.getCriticalItems().size());
        assertEquals("01.01", result.getCriticalItems().get(0).getItemCode());
        assertEquals(2500.0, result.getCriticalItems().get(0).getActualCost());
        assertEquals(1000.0, result.getCriticalItems().get(0).getEarnedValue());
        assertEquals(1500.0, result.getCriticalItems().get(0).getLossAmount());

        // Verificamos que se llamó solo 1 vez la consulta en lote y NUNCA en bucle
        verify(progressReportRepository, times(1)).sumEarnedValueByProjectIdGroupedByItemId(1L);
        verify(progressReportResourceRepository, times(1)).sumActualCostByProjectIdGroupedByItemId(1L);
    }
}
