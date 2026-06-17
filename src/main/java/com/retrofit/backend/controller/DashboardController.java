package com.retrofit.backend.controller;

import com.retrofit.backend.dto.ProjectDashboardResponseDto;
import com.retrofit.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    // Endpoint para el Dashboard de un proyecto específico
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ResponseEntity<ProjectDashboardResponseDto> getProjectDashboard(
            @PathVariable Long projectId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long itemId) {
        ProjectDashboardResponseDto response = dashboardService.getProjectDashboard(projectId, itemId);
        return ResponseEntity.ok(response);
    }
}
