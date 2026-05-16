package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.ProjectItemRequestDto;
import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.repository.ProgressReportRepository;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectRepository;
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
}