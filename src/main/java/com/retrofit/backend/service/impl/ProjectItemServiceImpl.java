package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.*;
import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.*;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.repository.*;
import com.retrofit.backend.service.ProjectItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectItemServiceImpl implements ProjectItemService {

    private final ProjectItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final ProgressReportRepository reportRepository;
    private final ResourceRepository resourceRepository;
    private final ProjectItemResourceRepository apuRepository;
    private final AuditService auditService;

    @Override
    public List<ProjectItemResponseDto> getItemsByProjectId(Long projectId) {

        List<ProjectItem> items = itemRepository.findByProjectIdOrderByItemOrderAsc(projectId);

        List<Object[]> executedResults = reportRepository.sumExecutedQuantityByProjectIdGroupedByItemId(projectId);
        Map<Long, Double> executedMap = new HashMap<>();
        for (Object[] result : executedResults) {
            executedMap.put((Long) result[0], ((Number) result[1]).doubleValue());
        }

        return items.stream().map(item -> {
            ProjectItemResponseDto dto = new ProjectItemResponseDto();
            dto.setId(item.getId());
            dto.setItemOrder(item.getItemOrder());
            dto.setCode(item.getCode());
            dto.setDescription(item.getDescription());
            dto.setUnit(item.getUnit());
            dto.setTotalQuantity(item.getTotalQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setLevel(item.getLevel());

            dto.setLaborYield(item.getLaborYield() != null ? item.getLaborYield() : 0.0);
            dto.setEquipmentYield(item.getEquipmentYield() != null ? item.getEquipmentYield() : 0.0);

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
                    apuDto.setResourceType(apu.getResource().fetchResourceType());
                    return apuDto;
                }).collect(Collectors.toList());
                dto.setApuDetails(apuDtos);
            } else {
                dto.setApuDetails(new ArrayList<>());
            }

            Double executed = executedMap.getOrDefault(item.getId(), 0.0);
            dto.setExecutedQuantity(executed);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditChange(action = "UPDATE", module = "Presupuestos")
    public List<ProjectItemResponseDto> saveBulkItems(Long projectId, BudgetSaveRequestDto request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        if (request.getGeneralExpensesPercentage() != null) {
            project.setGeneralExpensesPercentage(request.getGeneralExpensesPercentage());
        }
        if (request.getUtilityPercentage() != null) {
            project.setUtilityPercentage(request.getUtilityPercentage());
        }

        List<ProjectItem> itemsToSave = new ArrayList<>();
        int[] counters = new int[10];
        double calculatedDirectCost = 0.0;

        List<Long> itemIds = request.getItems().stream()
                .map(ProjectItemRequestDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, ProjectItem> existingItemsMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            itemRepository.findAllById(itemIds).forEach(item -> existingItemsMap.put(item.getId(), item));
        }

        for (ProjectItemRequestDto dto : request.getItems()) {
            ProjectItem item;
            if (dto.getId() != null) {
                item = existingItemsMap.getOrDefault(dto.getId(), new ProjectItem());
            } else {
                item = new ProjectItem();
            }

            item.setProject(project);

            item.setItemOrder(dto.getItemOrder() != null ? dto.getItemOrder() : 0);

            item.setDescription(dto.getDescription());
            item.setUnit(dto.getUnit());
            item.setTotalQuantity(dto.getTotalQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setLaborYield(dto.getLaborYield() != null ? dto.getLaborYield() : 0.0);
            item.setEquipmentYield(dto.getEquipmentYield() != null ? dto.getEquipmentYield() : 0.0);

            int level = dto.getLevel() != null ? dto.getLevel() : 0;
            item.setLevel(level);

            counters[level]++;
            for (int j = level + 1; j < counters.length; j++) {
                counters[j] = 0;
            }

            if (level == 0) {
                item.setCode(String.format("%2d.", counters[0]));
            } else {
                List<String> codeParts = new ArrayList<>();
                for (int i = 0; i <= level; i++) {
                    codeParts.add(String.valueOf(counters[i]));
                }
                item.setCode(String.join(".", codeParts));
            }

            if (item.getTotalQuantity() != null && item.getUnitPrice() != null) {
                calculatedDirectCost += (item.getTotalQuantity() * item.getUnitPrice());
            }

            itemsToSave.add(item);
        }

        itemRepository.saveAll(itemsToSave);

        double generalExpensesPercentage = project.getGeneralExpensesPercentage() != null ? project.getGeneralExpensesPercentage() : 5.0;
        double utilityPercentage = project.getUtilityPercentage() != null ? project.getUtilityPercentage() : 4.0;
        
        double generalExpenses = calculatedDirectCost * (generalExpensesPercentage / 100);
        double utility = calculatedDirectCost * (utilityPercentage / 100);
        double subtotal = calculatedDirectCost + generalExpenses + utility;
        double igv = subtotal * 0.18;
        
        project.setTotalBudget(subtotal + igv);
        projectRepository.save(project);

        return getItemsByProjectId(projectId);
    }

    @Override
    @Transactional
    @AuditChange(action = "UPDATE", module = "Presupuestos")
    public ProjectItemResponseDto saveApuDetails(Long itemId, Double laborYield, Double equipmentYield, List<ProjectItemResourceRequestDto> dtos) {

        // 1. Buscamos la Partida
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));


        item.setLaborYield(laborYield != null ? laborYield : 0.0);
        item.setEquipmentYield(equipmentYield != null ? equipmentYield : 0.0);
        item = itemRepository.saveAndFlush(item);

        // 2. Limpiamos el APU anterior
        apuRepository.deleteAll(item.getApuDetails());
        item.getApuDetails().clear();

        double calculatedUnitPrice = 0.0;

        // 3. LA MAGIA MATEMÁTICA
        for (ProjectItemResourceRequestDto dto : dtos) {
            Resource resource = resourceRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado"));

            ProjectItemResource pir = new ProjectItemResource();
            pir.setProjectItem(item);
            pir.setResource(resource);

            double quantity = 0.0;
            double partialPrice = 0.0;
            double squad = dto.getSquad() != null ? dto.getSquad() : 0.0;

            // FÓRMULA SEGÚN EL TIPO DE RECURSO
            if (resource instanceof LaborCategory) {
                pir.setSquad(squad);
                // Leemos directamente del parámetro asegurado para evitar delay de caché
                if (laborYield != null && laborYield > 0) {
                    quantity = (squad * 8.0) / laborYield;
                }
            } else if (resource instanceof Equipment) {
                pir.setSquad(squad);
                if (equipmentYield != null && equipmentYield > 0) {
                    quantity = (squad * 8.0) / equipmentYield;
                }
            } else if (resource instanceof Material) {
                pir.setSquad(0.0);
                quantity = dto.getQuantity() != null ? dto.getQuantity() : 0.0;
            }

            // Redondeo estándar
            quantity = Math.round(quantity * 10000.0) / 10000.0;
            pir.setQuantity(quantity);

            partialPrice = quantity * resource.getBasePrice();
            partialPrice = Math.round(partialPrice * 100.0) / 100.0;
            pir.setPartialPrice(partialPrice);

            calculatedUnitPrice += partialPrice;

            item.getApuDetails().add(pir);
        }

        // 4. ACTUALIZAMOS PRECIOS
        item.setUnitPrice(calculatedUnitPrice);
        itemRepository.saveAndFlush(item);

        // Recalcular el presupuesto total del proyecto
        recalculateProjectTotalBudget(item.getProject());

        if (item.getStartDate() != null && item.getTotalQuantity() != null && laborYield != null && laborYield > 0) {

            // 1. Calculamos la nueva duración real en días
            int newBaseDays = (int) Math.ceil(item.getTotalQuantity() / laborYield);

            // 2. Calculamos cuánto duraba antes en la base de datos (según sus fechas)
            long oldBaseDays = 1; // Por defecto
            if (item.getEndDate() != null) {
                oldBaseDays = ChronoUnit.DAYS.between(item.getStartDate(), item.getEndDate());
            }

            // 3. Si la duración cambió, disparamos la cascada
            if (newBaseDays != oldBaseDays) {
                item.setEndDate(item.getStartDate().plusDays(newBaseDays));
                itemRepository.saveAndFlush(item);

                long daysShifted = newBaseDays - oldBaseDays;
                cascadeDateShift(item.getId(), daysShifted);
            }
        }

        return getProjectItemById(itemId);
    }

    // 5. MÉTODO PARA RECALCULAR EL PROYECTO
    private void recalculateProjectTotalBudget(Project project) {
        double directCost = 0.0;
        for (ProjectItem pi : project.getItems()) {
            if (pi.getTotalQuantity() != null && pi.getUnitPrice() != null) {
                directCost += (pi.getTotalQuantity() * pi.getUnitPrice());
            }
        }
        
        double generalExpensesPercentage = project.getGeneralExpensesPercentage() != null ? project.getGeneralExpensesPercentage() : 5.0;
        double utilityPercentage = project.getUtilityPercentage() != null ? project.getUtilityPercentage() : 4.0;
        
        double generalExpenses = directCost * (generalExpensesPercentage / 100);
        double utility = directCost * (utilityPercentage / 100);
        double subtotal = directCost + generalExpenses + utility;
        double igv = subtotal * 0.18;
        
        project.setTotalBudget(subtotal + igv);
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

                apuDto.setResourceType(apu.getResource().fetchResourceType());
                return apuDto;
            }).collect(Collectors.toList());

            dto.setApuDetails(apuDtos);
        }
        return dto;
    }


    @Transactional
    public void updateGanttDates(Long itemId, GanttUpdateDto dto) {
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        // 1. Calculamos cuántos días se está moviendo la barra hacia el futuro o pasado
        long daysShifted = 0;
        if (item.getStartDate() != null && dto.getStartDate() != null) {
            daysShifted = ChronoUnit.DAYS.between(item.getStartDate(), dto.getStartDate());
        }

        // 2. Actualizamos la partida actual con las fechas que mandó Angular
        item.setStartDate(dto.getStartDate());
        item.setEndDate(dto.getEndDate());
        item.setPredecessorId(dto.getPredecessorId());
        
        List<ProjectItem> modifiedItems = new ArrayList<>();
        modifiedItems.add(item);

        // 3. EFECTO DOMINÓ: Si la barra se movió (daysShifted != 0), empujamos a sus hijas en memoria
        if (daysShifted != 0) {
            List<ProjectItem> allProjectItems = itemRepository.findByProjectId(item.getProject().getId());
            
            Map<Long, List<ProjectItem>> childrenGraph = new HashMap<>();
            for (ProjectItem pi : allProjectItems) {
                if (pi.getPredecessorId() != null) {
                    childrenGraph.computeIfAbsent(pi.getPredecessorId(), k -> new ArrayList<>()).add(pi);
                }
            }

            cascadeDateShiftInMemory(item.getId(), daysShifted, childrenGraph, modifiedItems);
        }
        
        itemRepository.saveAll(modifiedItems);
        
        // Registrar en el log de auditoría los cambios en el cronograma
        auditService.logAction("UPDATE", "Gantt", itemId, null, dto);
    }

    private void cascadeDateShiftInMemory(Long parentId, long daysShifted, Map<Long, List<ProjectItem>> childrenGraph, List<ProjectItem> modifiedItems) {
        List<ProjectItem> children = childrenGraph.getOrDefault(parentId, Collections.emptyList());

        for (ProjectItem child : children) {
            if (child.getStartDate() != null) {
                child.setStartDate(child.getStartDate().plusDays(daysShifted));
            }
            if (child.getEndDate() != null) {
                child.setEndDate(child.getEndDate().plusDays(daysShifted));
            }
            modifiedItems.add(child);

            // Si este hijo tiene otras partidas amarradas a él, la cascada continúa
            cascadeDateShiftInMemory(child.getId(), daysShifted, childrenGraph, modifiedItems);
        }
    }

    private void cascadeDateShift(Long parentId, long daysShifted) {
        ProjectItem item = itemRepository.findById(parentId).orElse(null);
        if (item == null) return;
        List<ProjectItem> allProjectItems = itemRepository.findByProjectId(item.getProject().getId());
        
        Map<Long, List<ProjectItem>> childrenGraph = new HashMap<>();
        for (ProjectItem pi : allProjectItems) {
            if (pi.getPredecessorId() != null) {
                childrenGraph.computeIfAbsent(pi.getPredecessorId(), k -> new ArrayList<>()).add(pi);
            }
        }
        
        List<ProjectItem> modifiedItems = new ArrayList<>();
        cascadeDateShiftInMemory(parentId, daysShifted, childrenGraph, modifiedItems);
        if (!modifiedItems.isEmpty()) {
            itemRepository.saveAll(modifiedItems);
        }
    }


    public List<GanttItemResponseDto> getGanttItems(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        List<ProjectItem> items = itemRepository.findByProjectId(projectId);
        // Respetamos el orden exacto del presupuesto
        items.sort(Comparator.comparing(ProjectItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProjectItem::getId));

        LocalDate currentDate = project.getStartDate();
        Long prevLeafItemId = null; // Guardará solo IDs de tareas "hijas"

        // 1. AUTO-LINKER: Solo para tareas reales (que tienen metrado)
        List<ProjectItem> itemsToUpdate = new ArrayList<>();
        
        for (ProjectItem item : items) {
            boolean isParent = (item.getTotalQuantity() == null || item.getTotalQuantity() == 0);

            if (!isParent && item.getStartDate() == null) {
                int baseDays = 1;
                if (item.getLaborYield() != null && item.getLaborYield() > 0) {
                    baseDays = (int) Math.ceil(item.getTotalQuantity() / item.getLaborYield());
                }

                item.setStartDate(currentDate);
                item.setEndDate(currentDate.plusDays(baseDays));
                item.setPredecessorId(prevLeafItemId); // Solo se amarra a la hija anterior
                itemsToUpdate.add(item);

                currentDate = item.getEndDate();
                prevLeafItemId = item.getId();
            }
        }
        
        if (!itemsToUpdate.isEmpty()) {
            itemRepository.saveAll(itemsToUpdate);
        }

        // 2. MAPEO CON JERARQUÍA (Padres e Hijos)
        Map<Integer, Long> levelTracker = new HashMap<>(); // Para rastrear quién es el padre actual de cada nivel

        return items.stream().map(item -> {
            int currentLevel = item.getLevel() != null ? item.getLevel() : 0;
            levelTracker.put(currentLevel, item.getId()); // Actualizamos el ID de este nivel

            // El padre siempre es el último elemento registrado en el nivel superior (currentLevel - 1)
            Long myParentId = currentLevel > 0 ? levelTracker.get(currentLevel - 1) : null;
            boolean isParent = (item.getTotalQuantity() == null || item.getTotalQuantity() == 0);

            GanttItemResponseDto dto = new GanttItemResponseDto();
            dto.setId(item.getId());
            dto.setName(item.getDescription());
            dto.setTotalQuantity(item.getTotalQuantity());
            dto.setLaborYield(item.getLaborYield());
            dto.setStartDate(item.getStartDate());
            dto.setEndDate(item.getEndDate());
            dto.setPredecessorId(item.getPredecessorId());
            dto.setCode(item.getCode());
            // --- INYECCIÓN DE JERARQUÍA ---
            dto.setParentId(myParentId);
            dto.setType(isParent ? "project" : "task");
            // ------------------------------

            int baseDays = 1;
            if (!isParent && item.getTotalQuantity() != null && item.getLaborYield() != null && item.getLaborYield() > 0) {
                baseDays = (int) Math.ceil(item.getTotalQuantity() / item.getLaborYield());
            }
            dto.setBaseDurationDays(isParent ? 0 : baseDays);

            Double executed = reportRepository.sumExecutedQuantityByItemId(item.getId());
            double progress = 0.0;
            if (!isParent && executed != null && item.getTotalQuantity() > 0) {
                progress = (executed / item.getTotalQuantity()) * 100;
            }
            dto.setCurrentProgressPercentage(progress);

            return dto;
        }).collect(Collectors.toList());
    }
}