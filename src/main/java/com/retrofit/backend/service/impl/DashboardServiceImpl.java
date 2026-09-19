package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.CriticalItemDto;
import com.retrofit.backend.dto.ProjectDashboardResponseDto;
import com.retrofit.backend.dto.TimeEvolutionDto;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.repository.ProgressReportRepository;
import com.retrofit.backend.repository.ProgressReportResourceRepository;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.service.DashboardService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final ProjectRepository  projectRepository;
    private final ProjectItemRepository projectItemRepository;
    private final ProgressReportRepository progressReportRepository;
    private final ProgressReportResourceRepository progressReportResourceRepository;


    @Override
    public ProjectDashboardResponseDto getProjectDashboard(Long projectId, Long itemId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado"));

        String exactCode = null;
        String prefixCode = null;
        if (itemId != null) {
            ProjectItem specificItem = projectItemRepository.findById(itemId).orElse(null);
            if (specificItem != null && specificItem.getCode() != null) {
                exactCode = specificItem.getCode().trim();
                if (exactCode.endsWith(".")) {
                    exactCode = exactCode.substring(0, exactCode.length() - 1);
                }
                prefixCode = exactCode + ".%";
            }
        }

        // KPIs PRINCIPALES
        Double pv = projectItemRepository.calculatePlannedValueByProjectId(projectId, exactCode, prefixCode);
        Double ev = progressReportRepository.calculateEarnedValueByProjectId(projectId, exactCode, prefixCode);
        Double ac = progressReportResourceRepository.calculateActualCostByProjectId(projectId, exactCode, prefixCode);

        Double cv = ev - ac;
        Double cpi = (ac == 0.0) ? 0.0 : (ev / ac);

        // AVANCES (Ejecutado vs Planificado)
        Double avanceTotalPlanificado = projectItemRepository.calculateTotalPlannedQuantityByProjectId(projectId, exactCode, prefixCode);
        Double avanceTotalEjecutado = progressReportRepository.calculateTotalExecutedQuantityByProjectId(projectId, exactCode, prefixCode);
        Double porcentajeAvance = (avanceTotalPlanificado == 0.0) ? 0.0 : ((avanceTotalEjecutado / avanceTotalPlanificado) * 100.0);
        
        String itemUnit = "";
        if (itemId != null) {
            ProjectItem specificItem = projectItemRepository.findById(itemId).orElse(null);
            if (specificItem != null && specificItem.getUnit() != null) {
                itemUnit = specificItem.getUnit();
            }
        }

        // TABLA DE ALERTAS (Optimizado: bulk queries agrupadas en vez de bucle N+1)
        List<CriticalItemDto> criticalItems = new ArrayList<>();
        if (ac != null && ac > 0.0) {
            List<ProjectItem> items;
            List<ProjectItem> allProjectItems;
            if (itemId != null) {
                ProjectItem specificItem = projectItemRepository.findById(itemId).orElse(null);
                items = specificItem != null ? List.of(specificItem) : Collections.emptyList();
                allProjectItems = projectItemRepository.findByProjectId(projectId);
            } else {
                items = projectItemRepository.findByProjectId(projectId);
                allProjectItems = items;
            }

            if (!items.isEmpty()) {
                List<Object[]> evRows = progressReportRepository.sumEarnedValueByProjectIdGroupedByItemId(projectId);
                Map<Long, Double> rawEvByItem = new HashMap<>();
                for (Object[] row : evRows) {
                    Long idKey = (Long) row[0];
                    Double val = (Double) row[1];
                    rawEvByItem.put(idKey, val != null ? val : 0.0);
                }

                List<Object[]> acRows = progressReportResourceRepository.sumActualCostByProjectIdGroupedByItemId(projectId);
                Map<Long, Double> rawAcByItem = new HashMap<>();
                for (Object[] row : acRows) {
                    Long idKey = (Long) row[0];
                    Double val = (Double) row[1];
                    rawAcByItem.put(idKey, val != null ? val : 0.0);
                }

                for (ProjectItem item : items) {
                    String code = item.getCode() != null ? item.getCode().trim() : "";
                    if (code.endsWith(".")) {
                        code = code.substring(0, code.length() - 1);
                    }
                    String prefix = code + ".";

                    double itemEv = 0.0;
                    double itemAc = 0.0;

                    for (ProjectItem target : allProjectItems) {
                        String targetCode = target.getCode() != null ? target.getCode().trim() : "";
                        if (targetCode.equals(code) || targetCode.startsWith(prefix)) {
                            itemEv += rawEvByItem.getOrDefault(target.getId(), 0.0);
                            itemAc += rawAcByItem.getOrDefault(target.getId(), 0.0);
                        }
                    }

                    if (itemAc > itemEv) {
                        criticalItems.add(CriticalItemDto.builder()
                                .itemId(item.getId())
                                .itemCode(item.getCode())
                                .description(item.getDescription())
                                .earnedValue(itemEv)
                                .actualCost(itemAc)
                                .lossAmount(itemAc - itemEv)
                                .build());
                    }
                }
            }
        }

        // GRÁFICO DE DONA (Distribuir AC por el tipo de clase hija)
        Double totalLaborCost = 0.0;
        Double totalMaterialCost = 0.0;
        Double totalEquipmentCost = 0.0;

        if (ac != null && ac > 0.0) {
            List<Object[]> costsByType = progressReportResourceRepository.calculateActualCostByResourceType(projectId, exactCode, prefixCode);
            for (Object[] row : costsByType) {
                Class<?> resourceClass = (Class<?>) row[0]; // Retorna ej: LaborCategory.class
                Double cost = (Double) row[1];

                String className = resourceClass != null ? resourceClass.getSimpleName().toUpperCase() : "";
                if (className.contains("LABOR")) {
                    totalLaborCost += cost;
                } else if (className.contains("MATERIAL")) {
                    totalMaterialCost += cost;
                } else if (className.contains("EQUIPMENT")) {
                    totalEquipmentCost += cost;
                }
            }
        }

        // CURVA S (Evolución acumulada en el tiempo)
        List<TimeEvolutionDto> timeEvolution = new ArrayList<>();
        if ((ev != null && ev > 0.0) || (ac != null && ac > 0.0)) {
            List<Object[]> evData = progressReportRepository.getEarnedValueByDate(projectId, exactCode, prefixCode);
            List<Object[]> acData = progressReportResourceRepository.getActualCostByDate(projectId, exactCode, prefixCode);

            Map<LocalDate, double[]> timelineData = new TreeMap<>();

            for (Object[] row : evData) {
                LocalDate date = (LocalDate) row[0];
                Double value = (Double) row[1];
                timelineData.putIfAbsent(date, new double[]{0.0, 0.0});
                timelineData.get(date)[0] = value; // Índice 0 para EV
            }

            for (Object[] row : acData) {
                LocalDate date = (LocalDate) row[0];
                Double value = (Double) row[1];
                timelineData.putIfAbsent(date, new double[]{0.0, 0.0});
                timelineData.get(date)[1] = value; // Índice 1 para AC
            }

            Double currentEvAcc = 0.0;
            Double currentAcAcc = 0.0;

            for (Map.Entry<LocalDate, double[]> entry : timelineData.entrySet()) {
                currentEvAcc += entry.getValue()[0];
                currentAcAcc += entry.getValue()[1];

                timeEvolution.add(TimeEvolutionDto.builder()
                        .dateLabel(entry.getKey().toString())
                        .plannedValueAccumulated(pv) // PV suele ser la meta (línea recta superior)
                        .earnedValueAccumulated(currentEvAcc)
                        .actualCostAccumulated(currentAcAcc)
                        .build());
            }
        }

        // ULTIMOS AVANCES (Para la vista de "Avances del Ultimo Dia")
        List<com.retrofit.backend.dto.LatestProgressDto> recentProgresses = Collections.emptyList();
        if (avanceTotalEjecutado != null && avanceTotalEjecutado > 0.0) {
            org.springframework.data.domain.Pageable topFive = org.springframework.data.domain.PageRequest.of(0, 5);
            List<com.retrofit.backend.model.ProgressReport> recentReports = progressReportRepository.findRecentReports(projectId, exactCode, prefixCode, topFive);
            recentProgresses = recentReports.stream().map(pr -> com.retrofit.backend.dto.LatestProgressDto.builder()
                    .reportId(pr.getId())
                    .date(pr.getReportDate().toString())
                    .itemCode(pr.getProjectItem().getCode())
                    .itemDescription(pr.getProjectItem().getDescription())
                    .executedQuantity(pr.getExecutedQuantity())
                    .unit(pr.getProjectItem().getUnit())
                    .build()
            ).toList();
        }

        return ProjectDashboardResponseDto.builder()
                .projectId(project.getId())
                .projectCode(project.getCode())
                .projectName(project.getName())
                .plannedValue(pv)
                .earnedValue(ev)
                .actualCost(ac)
                .costVariance(cv)
                .cpi(cpi)
                .avanceTotalEjecutado(avanceTotalEjecutado)
                .avanceTotalPlanificado(avanceTotalPlanificado)
                .porcentajeAvance(porcentajeAvance)
                .itemUnit(itemUnit)
                .totalLaborCost(totalLaborCost)
                .totalMaterialCost(totalMaterialCost)
                .totalEquipmentCost(totalEquipmentCost)
                .criticalItems(criticalItems)
                .timeEvolution(timeEvolution)
                .recentProgresses(recentProgresses)
                .build();
    }

    @Override
    public ProjectDashboardResponseDto getGlobalDashboard() {
        return null;
    }
}
