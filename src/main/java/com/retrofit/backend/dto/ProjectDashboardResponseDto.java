package com.retrofit.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectDashboardResponseDto {
    private Long projectId;
    private String projectCode;
    private String projectName;

    // Kpis principales
    private Double plannedValue; // PV
    private Double earnedValue; // EV
    private Double actualCost; // AC
    private Double costVariance; // CV (EV - AC)
    private Double cpi; // Indice de desempeno (EV / AC)

    // Avances
    private Double avanceTotalEjecutado;
    private Double avanceTotalPlanificado;
    private Double porcentajeAvance;
    private String itemUnit; // Unidad de la partida o "" si es global

    // Para el grafico de donas
    private Double totalLaborCost; // Mano de obra
    private Double totalMaterialCost; // Materiales
    private Double totalEquipmentCost; // Equipos

    // Para el grafico de lineas Curva S (Evolucion)
    private List<TimeEvolutionDto> timeEvolution;

    // Para la tabla de alertias Partidas Criticas
    private List<CriticalItemDto> criticalItems;

    // Para la lista de ultimos avances
    private List<LatestProgressDto> recentProgresses;
}
