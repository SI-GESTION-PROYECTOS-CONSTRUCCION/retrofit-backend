package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectDashboardResponseDto;

public interface DashboardService {
    // Trae los datos detallados de un proyecto en específico (Curva S, Donas, Alertas)
    ProjectDashboardResponseDto getProjectDashboard(Long projectId);

    // Trae los datos resumidos globales de la empresa (Para la vista "Todos")
    ProjectDashboardResponseDto getGlobalDashboard();
}
