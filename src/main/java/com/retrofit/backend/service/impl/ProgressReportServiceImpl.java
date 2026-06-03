package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.GroupedProgressReportDto;
import com.retrofit.backend.dto.ProgressReportRequestDto;
import com.retrofit.backend.dto.ProgressReportResponseDto;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.*;
import com.retrofit.backend.repository.*;
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
    private final ProgressReportResourceRepository progressReportResourceRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public void createReportWithPhotos(ProgressReportRequestDto dto, List<MultipartFile> files) {

        // 1. BUSCAR LA PARTIDA
        ProjectItem item = itemRepository.findById(dto.getProjectItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (item.getProject().getStatus() == ProjectStatus.COMPLETED || item.getProject().getCurrentProgress() >= 100.0) {
            throw new IllegalStateException("El registro está cerrado. Este proyecto ya ha sido completado al 100%.");
        }
        // 2. VALIDACIÓN LÓGICA CRÍTICA (REGLA DE NEGOCIO)
        Double alreadyExecuted = reportRepository.sumExecutedQuantityByItemId(item.getId());
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
                        .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + resDto.getResourceId()));

                ProgressReportResource prr = new ProgressReportResource();
                prr.setProgressReport(report);
                prr.setResource(resource);

                prr.setTheoreticalQuantity(resDto.getTheoreticalQuantity() != null ? resDto.getTheoreticalQuantity() : 0.0);
                prr.setRealQuantity(resDto.getRealQuantity() != null ? resDto.getRealQuantity() : 0.0);

                report.getUsedResources().add(prr);
            }
        }

        // Guardamos todo en cascada (El reporte y sus recursos hijos)
        ProgressReport savedReport = reportRepository.save(report);

        // 4. PROCESAR Y GUARDAR FOTOS
        if (files != null && !files.isEmpty()) {
            List<ProgressPhoto> photosToSave = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    // El StorageService guarda en disco y devuelve la URL local (ej. http://localhost...)
                    String fileUrl = storageService.store(file);

                    ProgressPhoto photo = new ProgressPhoto();
                    photo.setProgressReport(savedReport);
                    photo.setFileName(file.getOriginalFilename());
                    photo.setFileSizeBytes(file.getSize());
                    photo.setFileUrl(fileUrl);
                    photosToSave.add(photo);
                }
            }
            photoRepository.saveAll(photosToSave);
        }

        // 5. ACTUALIZAR PROGRESO GLOBAL DEL PROYECTO
        updateProjectOverallProgress(item.getProject());
    }

    private void updateProjectOverallProgress(Project project) {
        List<ProjectItem> allItems = project.getItems();
        if (allItems == null || allItems.isEmpty()) return;

        double totalProgressSum = 0;
        int validItemsCount = 0;

        // Calculamos el % de avance de cada partida y sacamos el promedio de la obra
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
            // Si llegó al 100%, se completa automáticamente
            project.setStatus(ProjectStatus.COMPLETED);
        } else if (projectProgress > 0.0) {
            // Si ya avanzó algo, y seguía en fase de planeamiento, pasa a ejecución
            if (project.getStatus() == ProjectStatus.PLANNING) {
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            // (Si está ON_HOLD no lo tocamos, respetamos la pausa de gerencia)
        } else {
            // Si por alguna razón el progreso vuelve a 0 (ej. eliminaron el único reporte)
            project.setStatus(ProjectStatus.PLANNING);
        }

        projectRepository.save(project);
    }


    public List<GroupedProgressReportDto> getFilteredAndGroupedReports(Long projectId, LocalDate startDate, LocalDate endDate, String itemCode) {
        String codeFilter = (itemCode != null && !itemCode.trim().isEmpty()) ? "%" + itemCode.trim() + "%" : null;
        List<ProgressReport> reports = reportRepository.findFilteredReports(projectId, startDate, endDate, codeFilter);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", new java.util.Locale("es", "ES"));

        java.util.Map<String, List<ProgressReportResponseDto>> groupedMap = reports.stream().map(report -> {
            // Mapeo básico que ya tenías
            ProgressReportResponseDto dto = new ProgressReportResponseDto();
            dto.setId(report.getId());
            dto.setItemCode(report.getProjectItem().getCode());
            dto.setItemDescription(report.getProjectItem().getDescription());
            dto.setReportDate(report.getReportDate());
            dto.setExecutedQuantity(report.getExecutedQuantity());
            dto.setUnit(report.getProjectItem().getUnit());
            dto.setObservations(report.getObservations());


            if (report.getUsedResources() != null && !report.getUsedResources().isEmpty()) {
                List<com.retrofit.backend.dto.ProgressReportResourceResponseDto> resourceDtos = report.getUsedResources().stream().map(prr -> {
                    com.retrofit.backend.dto.ProgressReportResourceResponseDto resDto = new com.retrofit.backend.dto.ProgressReportResourceResponseDto();
                    resDto.setId(prr.getId());
                    resDto.setResourceId(prr.getResource().getId());
                    resDto.setResourceName(prr.getResource().getName());
                    resDto.setResourceUnit(prr.getResource().getUnit());
                    resDto.setTheoreticalQuantity(prr.getTheoreticalQuantity());
                    resDto.setRealQuantity(prr.getRealQuantity());

                    // Magia del Polimorfismo que programamos antes
                    resDto.setResourceType(prr.getResource().fetchResourceType());

                    return resDto;
                }).collect(Collectors.toList());

                dto.setUsedResources(resourceDtos);
            }

            // Fotos (que ya tenías)
            if (report.getPhotos() != null) {
                dto.setPhotoUrls(report.getPhotos().stream()
                        .map(ProgressPhoto::getFileUrl)
                        .collect(Collectors.toList()));
            }
            return dto;
        }).collect(Collectors.groupingBy(

                dto -> dto.getReportDate().format(formatter).toUpperCase(),

                java.util.LinkedHashMap::new,
                Collectors.toList()
        ));


        List<GroupedProgressReportDto> result = new ArrayList<>();
        for (java.util.Map.Entry<String, List<ProgressReportResponseDto>> entry : groupedMap.entrySet()) {
            result.add(new GroupedProgressReportDto(entry.getKey(), entry.getValue()));
        }

        return result;
    }
}