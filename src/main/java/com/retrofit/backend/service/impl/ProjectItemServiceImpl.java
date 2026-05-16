package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectItemRequestDto;
import com.retrofit.backend.dto.ProjectItemResourceRequestDto;
import com.retrofit.backend.dto.ProjectItemResourceResponseDto;
import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.*;
import com.retrofit.backend.repository.*;
import com.retrofit.backend.service.ProjectItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectItemServiceImpl implements ProjectItemService {

    private final ProjectItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final ProgressReportRepository reportRepository;
    private final ResourceRepository resourceRepository;
    private final ProjectItemResourceRepository apuRepository;

    @Override
    public List<ProjectItemResponseDto> getItemsByProjectId(Long projectId) {
        List<ProjectItem> items = itemRepository.findByProjectId(projectId);

        return items.stream().map(item -> {
            ProjectItemResponseDto dto = new ProjectItemResponseDto();
            dto.setId(item.getId());
            dto.setCode(item.getCode());
            dto.setDescription(item.getDescription());
            dto.setUnit(item.getUnit());
            dto.setTotalQuantity(item.getTotalQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setLevel(item.getLevel());
            // Calculamos cuánto se ha avanzado hasta hoy
            Double executed = reportRepository.sumExecutedQuantityByItemId(item.getId());
            dto.setExecutedQuantity(executed != null ? executed : 0.0);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ProjectItemResponseDto> saveBulkItems(Long projectId, List<ProjectItemRequestDto> dtos) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        List<ProjectItem> itemsToSave = new ArrayList<>();

        int[] counters = new int[10];
        double calculatedTotalBudget = 0.0;
        for (ProjectItemRequestDto dto : dtos) {
            ProjectItem item = new ProjectItem();
            item.setProject(project);
            item.setDescription(dto.getDescription());
            item.setUnit(dto.getUnit());
            item.setTotalQuantity(dto.getTotalQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setLaborYield(dto.getLaborYield() != null ? dto.getLaborYield() : 0.0);
            item.setEquipmentYield(dto.getEquipmentYield() != null ? dto.getEquipmentYield() : 0.0);

            // Obtenemos el nivel de sangría (por defecto 0)
            int level = dto.getLevel() != null ? dto.getLevel() : 0;
            item.setLevel(level);

            // --- LÓGICA DE CÁLCULO WBS EN EL BACKEND ---
            counters[level]++;

            // Reseteamos a cero los niveles inferiores
            for (int j = level + 1; j < counters.length; j++) {
                counters[j] = 0;
            }

            // Construimos el código
            if (level == 0) {
                // Título principal: "1.", "2."
                item.setCode(String.format("%2d.", counters[0]));
            } else {
                // Sub-partidas: "1.1", "1.1.1"
                List<String> codeParts = new ArrayList<>();
                for (int i = 0; i <= level; i++) {
                    codeParts.add(String.valueOf(counters[i]));
                }
                item.setCode(String.join(".", codeParts));
            }

            if (item.getTotalQuantity() != null && item.getUnitPrice() != null) {
                calculatedTotalBudget += (item.getTotalQuantity() * item.getUnitPrice());
            }

            itemsToSave.add(item);
        }

        itemRepository.saveAll(itemsToSave);
        project.setTotalBudget(calculatedTotalBudget);
        projectRepository.save(project);
        return getItemsByProjectId(projectId);
    }

    @Override
    @Transactional
    public ProjectItemResponseDto saveApuDetails(Long itemId, List<ProjectItemResourceRequestDto> dtos) {

        // 1. Buscamos la Partida
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        // 2. Limpiamos el APU anterior (si el ingeniero está editando el presupuesto, borramos el viejo)
        apuRepository.deleteAll(item.getApuDetails());
        item.getApuDetails().clear();

        double calculatedUnitPrice = 0.0;

        // 3. LA MAGIA MATEMÁTICA
        for (ProjectItemResourceRequestDto dto : dtos) {
            // Buscamos el recurso en el catálogo maestro
            Resource resource = resourceRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado"));

            ProjectItemResource pir = new ProjectItemResource();
            pir.setProjectItem(item);
            pir.setResource(resource);

            double quantity = 0.0;
            double partialPrice = 0.0;
            double squad = dto.getSquad() != null ? dto.getSquad() : 0.0;

            // FÓRMULA SEGÚN EL TIPO DE RECURSO
            if (resource instanceof LaborCategory) { // MANO DE OBRA
                pir.setSquad(squad);
                if (item.getLaborYield() != null && item.getLaborYield() > 0) {
                    // Fórmula: (Cuadrilla * 8 horas) / Rendimiento
                    quantity = (squad * 8.0) / item.getLaborYield();
                }
            } else if (resource instanceof Equipment) { // MAQUINARIA
                pir.setSquad(squad);
                if (item.getEquipmentYield() != null && item.getEquipmentYield() > 0) {
                    // Fórmula: (Cuadrilla * 8 horas) / Rendimiento
                    quantity = (squad * 8.0) / item.getEquipmentYield();
                }
            } else if (resource instanceof Material) { // MATERIALES
                pir.setSquad(0.0); // Los materiales no tienen cuadrilla
                // En materiales, la cantidad la digita directamente el ingeniero
                quantity = dto.getQuantity() != null ? dto.getQuantity() : 0.0;
            }

            // REDONDEO Y CÁLCULO DE PARCIAL
            // Redondeamos la cantidad a 4 decimales (Estándar S10)
            quantity = Math.round(quantity * 10000.0) / 10000.0;
            pir.setQuantity(quantity);

            // Calculamos el dinero: Cantidad * Precio Base
            partialPrice = quantity * resource.getBasePrice();
            partialPrice = Math.round(partialPrice * 100.0) / 100.0; // 2 decimales para moneda
            pir.setPartialPrice(partialPrice);

            // Sumamos al precio unitario total de la partida
            calculatedUnitPrice += partialPrice;

            item.getApuDetails().add(pir);
        }

        // 4. ACTUALIZAMOS EL PRECIO DE LA PARTIDA Y DEL PROYECTO GLOBAL
        item.setUnitPrice(calculatedUnitPrice);
        itemRepository.save(item);

        // Recalcular el presupuesto total del proyecto (porque una partida cambió su precio)
        recalculateProjectTotalBudget(item.getProject());

        // Retornas tu DTO (Asegúrate de tener un método de mapeo, o devuelve lo que necesites)
        return getProjectItemById(itemId);
    }

    // 5. MÉTODO PARA RECALCULAR EL PROYECTO
    private void recalculateProjectTotalBudget(Project project) {
        double totalBudget = 0.0;
        for (ProjectItem pi : project.getItems()) {
            if (pi.getTotalQuantity() != null && pi.getUnitPrice() != null) {
                totalBudget += (pi.getTotalQuantity() * pi.getUnitPrice());
            }
        }
        project.setTotalBudget(totalBudget);
        projectRepository.save(project);
    }

    @Override
    public ProjectItemResponseDto getProjectItemById(Long itemId) {
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        ProjectItemResponseDto dto = new ProjectItemResponseDto();
        dto.setId(item.getId());
        dto.setCode(item.getCode());
        dto.setDescription(item.getDescription());
        dto.setUnit(item.getUnit());
        dto.setTotalQuantity(item.getTotalQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLevel(item.getLevel());

        // Mapeo de los rendimientos
        dto.setLaborYield(item.getLaborYield());
        dto.setEquipmentYield(item.getEquipmentYield());

        // Calculamos cuánto se ha avanzado hasta hoy
        Double executed = reportRepository.sumExecutedQuantityByItemId(item.getId());
        dto.setExecutedQuantity(executed != null ? executed : 0.0);
        if (item.getApuDetails() != null && !item.getApuDetails().isEmpty()) {
            List<ProjectItemResourceResponseDto> apuDtos = item.getApuDetails().stream().map(apu -> {
                ProjectItemResourceResponseDto apuDto = new ProjectItemResourceResponseDto();
                apuDto.setId(apu.getId());
                apuDto.setResourceId(apu.getResource().getId());
                apuDto.setResourceName(apu.getResource().getName());
                apuDto.setResourceUnit(apu.getResource().getUnit());
                apuDto.setResourceBasePrice(apu.getResource().getBasePrice());
                apuDto.setSquad(apu.getSquad());
                apuDto.setQuantity(apu.getQuantity());
                apuDto.setPartialPrice(apu.getPartialPrice());

                // Identificamos el tipo para que Angular pueda separarlos por colores o tablas
                if (apu.getResource() instanceof LaborCategory) {
                    apuDto.setResourceType("LABOR");
                } else if (apu.getResource() instanceof Equipment) {
                    apuDto.setResourceType("EQUIPMENT");
                } else {
                    apuDto.setResourceType("MATERIAL");
                }

                return apuDto;
            }).collect(Collectors.toList());

            dto.setApuDetails(apuDtos);
        }
        return dto;
    }
}