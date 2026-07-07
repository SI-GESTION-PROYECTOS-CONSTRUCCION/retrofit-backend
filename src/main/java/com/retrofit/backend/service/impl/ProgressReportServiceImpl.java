package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.GroupedProgressReportDto;
import com.retrofit.backend.dto.ProgressReportRequestDto;
import com.retrofit.backend.dto.ProgressReportResponseDto;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.*;
import com.retrofit.backend.repository.*;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.ProgressReportService;
import com.retrofit.backend.service.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressReportServiceImpl implements ProgressReportService {

    private final ProjectItemRepository itemRepository;
    private final ProgressReportRepository reportRepository;
    private final ProgressPhotoRepository photoRepository;
    private final ProjectRepository projectRepository;
    private final StorageService storageService;
    private final ResourceRepository resourceRepository;
    private final AuditService auditService;

    @Transactional
    public void createReportWithPhotos(ProgressReportRequestDto dto, List<MultipartFile> files) {

        // 1. BUSCAR LA PARTIDA
        ProjectItem item = itemRepository.findById(dto.getProjectItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (item.getProject().getStatus() == ProjectStatus.COMPLETED
                || item.getProject().getCurrentProgress() >= 100.0) {
            throw new IllegalStateException("El registro está cerrado. Este proyecto ya ha sido completado al 100%.");
        }

        // 2. VALIDACIÓN LÓGICA CRÍTICA
        Double alreadyExecuted = reportRepository.sumExecutedQuantityByItemId(item.getId());
        if (alreadyExecuted == null)
            alreadyExecuted = 0.0; // Prevención de nulos
        Double remainingQuantity = item.getTotalQuantity() - alreadyExecuted;

        if (dto.getExecutedQuantity() > remainingQuantity) {
            throw new IllegalArgumentException("El metrado reportado (" + dto.getExecutedQuantity() +
                    ") excede el saldo disponible de la partida (" + remainingQuantity + ").");
        }

        // 3. CREAR EL AVANCE
        ProgressReport report = new ProgressReport();
        report.setProjectItem(item);
        report.setReportDate(dto.getReportDate());
        report.setExecutedQuantity(dto.getExecutedQuantity());
        report.setObservations(dto.getObservations());

        if (dto.getUsedResources() != null && !dto.getUsedResources().isEmpty()) {
            for (com.retrofit.backend.dto.ProgressReportResourceRequestDto resDto : dto.getUsedResources()) {

                Resource resource = resourceRepository.findById(resDto.getResourceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Recurso no encontrado: " + resDto.getResourceId()));

                ProgressReportResource prr = new ProgressReportResource();
                prr.setProgressReport(report);
                prr.setResource(resource);

                prr.setTheoreticalQuantity(
                        resDto.getTheoreticalQuantity() != null ? resDto.getTheoreticalQuantity() : 0.0);
                prr.setRealQuantity(resDto.getRealQuantity() != null ? resDto.getRealQuantity() : 0.0);

                report.getUsedResources().add(prr);
            }
        }

        ProgressReport savedReport = reportRepository.save(report);

        // 4. PROCESAR Y GUARDAR FOTOS
        List<ProgressPhoto> savedPhotos = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileUrl = storageService.store(file);
                    ProgressPhoto photo = new ProgressPhoto();
                    photo.setProgressReport(savedReport);
                    photo.setFileName(file.getOriginalFilename());
                    photo.setFileSizeBytes(file.getSize());
                    photo.setFileUrl(fileUrl);
                    savedPhotos.add(photo);
                }
            }
            photoRepository.saveAll(savedPhotos);
            savedReport.setPhotos(savedPhotos); // Se las asignamos al reporte para que salgan en el JSON
        }

        // 5. ACTUALIZAR PROGRESO GLOBAL DEL PROYECTO
        updateProjectOverallProgress(item.getProject());

        // 6. GRABAR AUDITORÍA MANUALMENTE
        // Usamos nuestro método refactorizado para que el JSON quede limpio y perfecto
        ProgressReportResponseDto estadoNuevo = mapToResponseDto(savedReport);
        auditService.logAction("CREATE", "Avances de Obra", savedReport.getId(), null, estadoNuevo);
    }

    private void updateProjectOverallProgress(Project project) {
        List<ProjectItem> allItems = project.getItems();
        if (allItems == null || allItems.isEmpty())
            return;

        double totalProgressSum = 0;
        int validItemsCount = 0;

        for (ProjectItem i : allItems) {
            if (i.getTotalQuantity() == null || i.getTotalQuantity() == 0) {
                continue;
            }

            Double executed = reportRepository.sumExecutedQuantityByItemId(i.getId());
            if (executed == null) {
                executed = 0.0;
            }

            double itemPercentage = (executed / i.getTotalQuantity()) * 100;
            totalProgressSum += itemPercentage;

            validItemsCount++;
        }

        if (validItemsCount == 0) {
            project.setCurrentProgress(0.0);
            project.setStatus(ProjectStatus.PLANNING);
            projectRepository.save(project);
            return;
        }

        double projectProgress = totalProgressSum / validItemsCount;
        projectProgress = Math.round(projectProgress * 100.0) / 100.0;

        project.setCurrentProgress(projectProgress);

        if (projectProgress >= 100.0) {
            project.setStatus(ProjectStatus.COMPLETED);
        } else if (projectProgress > 0.0) {
            if (project.getStatus() == ProjectStatus.PLANNING) {
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
        } else {
            project.setStatus(ProjectStatus.PLANNING);
        }

        projectRepository.save(project);
    }

    public List<GroupedProgressReportDto> getFilteredAndGroupedReports(Long projectId, LocalDate startDate,
            LocalDate endDate, String itemCode) {
        String codeFilter = (itemCode != null && !itemCode.trim().isEmpty()) ? "%" + itemCode.trim() + "%" : null;
        List<ProgressReport> reports = reportRepository.findFilteredReports(projectId, startDate, endDate, codeFilter);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",
                new java.util.Locale("es", "ES"));

        java.util.Map<String, List<ProgressReportResponseDto>> groupedMap = reports.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.groupingBy(
                        dto -> dto.getReportDate().format(formatter).toUpperCase(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));

        List<GroupedProgressReportDto> result = new ArrayList<>();
        for (java.util.Map.Entry<String, List<ProgressReportResponseDto>> entry : groupedMap.entrySet()) {
            result.add(new GroupedProgressReportDto(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    // MÉTODO REFACTORIZADO (Sirve para las búsquedas y para la auditoría)
    private ProgressReportResponseDto mapToResponseDto(ProgressReport report) {
        ProgressReportResponseDto dto = new ProgressReportResponseDto();
        dto.setId(report.getId());
        dto.setItemCode(report.getProjectItem().getCode());
        dto.setItemDescription(report.getProjectItem().getDescription());
        dto.setReportDate(report.getReportDate());
        dto.setExecutedQuantity(report.getExecutedQuantity());
        dto.setUnit(report.getProjectItem().getUnit());
        dto.setObservations(report.getObservations());

        if (report.getUsedResources() != null && !report.getUsedResources().isEmpty()) {
            List<com.retrofit.backend.dto.ProgressReportResourceResponseDto> resourceDtos = report.getUsedResources()
                    .stream().map(prr -> {
                        com.retrofit.backend.dto.ProgressReportResourceResponseDto resDto = new com.retrofit.backend.dto.ProgressReportResourceResponseDto();
                        resDto.setId(prr.getId());
                        resDto.setResourceId(prr.getResource().getId());
                        resDto.setResourceName(prr.getResource().getName());
                        resDto.setResourceUnit(prr.getResource().getUnit());
                        resDto.setTheoreticalQuantity(prr.getTheoreticalQuantity());
                        resDto.setRealQuantity(prr.getRealQuantity());
                        resDto.setResourceType(prr.getResource().fetchResourceType());
                        return resDto;
                    }).collect(Collectors.toList());

            dto.setUsedResources(resourceDtos);
        }

        if (report.getPhotos() != null) {
            dto.setPhotoUrls(report.getPhotos().stream()
                    .map(ProgressPhoto::getFileUrl)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}