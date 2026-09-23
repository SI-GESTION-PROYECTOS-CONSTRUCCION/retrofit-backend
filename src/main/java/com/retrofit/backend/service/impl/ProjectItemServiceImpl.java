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
    private final ProjectItemDependencyRepository dependencyRepository;
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

        if (project.getCurrentProgress() != null && project.getCurrentProgress() > 0) {
            throw new IllegalStateException("No se puede modificar el presupuesto de una obra en ejecución.");
        }

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

        // Eliminar items que ya no están en la lista (fueron borrados en el frontend)
        List<ProjectItem> currentItemsInDb = itemRepository.findByProjectId(projectId);
        List<ProjectItem> itemsToDelete = currentItemsInDb.stream()
                .filter(item -> !itemIds.contains(item.getId()))
                .collect(Collectors.toList());

        if (!itemsToDelete.isEmpty()) {
            itemRepository.deleteAll(itemsToDelete);
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

        double generalExpensesPercentage = project.getGeneralExpensesPercentage() != null
                ? project.getGeneralExpensesPercentage()
                : 5.0;
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
    public ProjectItemResponseDto saveApuDetails(Long itemId, Double laborYield, Double equipmentYield,
            List<ProjectItemResourceRequestDto> dtos) {

        // 1. Buscamos la Partida
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (item.getProject() != null && item.getProject().getCurrentProgress() != null && item.getProject().getCurrentProgress() > 0) {
            throw new IllegalStateException("No se puede modificar el APU de una obra en ejecución.");
        }

        item.setLaborYield(laborYield != null ? laborYield : 0.0);
        item.setEquipmentYield(equipmentYield != null ? equipmentYield : 0.0);
        item = itemRepository.saveAndFlush(item);

        // 2. Limpiamos el APU anterior
        apuRepository.deleteAll(item.getApuDetails());
        item.getApuDetails().clear();

        double calculatedUnitPrice = 0.0;

        // 3. Pre-cargar recursos en batch para evitar N+1 queries en bucle
        Set<Long> resourceIds = dtos.stream()
                .map(ProjectItemResourceRequestDto::getResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Resource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(Resource::getId, r -> r));

        // 4. LA MAGIA MATEMÁTICA
        for (ProjectItemResourceRequestDto dto : dtos) {
            Resource resource = resourceMap.get(dto.getResourceId());
            if (resource == null) {
                throw new ResourceNotFoundException("Recurso no encontrado: " + dto.getResourceId());
            }

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

        double generalExpensesPercentage = project.getGeneralExpensesPercentage() != null
                ? project.getGeneralExpensesPercentage()
                : 5.0;
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
    public void updateGanttDates(Long projectId, Long itemId, GanttUpdateDto dto) {
        ProjectItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (!item.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("La partida no pertenece al proyecto especificado.");
        }

        List<ProjectItem> allProjectItems = itemRepository.findByProjectId(projectId);
        Map<Long, ProjectItem> itemMap = allProjectItems.stream()
                .collect(Collectors.toMap(ProjectItem::getId, p -> p));

        List<GanttDependencyDto> requestedDependencies = dto.getDependencies();
        if (requestedDependencies == null) {
            requestedDependencies = dto.getPredecessorId() == null ? List.of() : List.of(legacyDependency(dto.getPredecessorId()));
        }
        validateDependencies(item, requestedDependencies, itemMap);

        Map<Long, List<GanttDependencyDto>> relations = new HashMap<>();
        for (ProjectItemDependency relation : dependencyRepository.findBySuccessorProjectId(projectId)) {
            GanttDependencyDto link = new GanttDependencyDto();
            link.setPredecessorId(relation.getPredecessor().getId());
            link.setType(relation.getType());
            relations.computeIfAbsent(relation.getSuccessor().getId(), ignored -> new ArrayList<>()).add(link);
        }
        for (ProjectItem existing : allProjectItems) {
            if (!relations.containsKey(existing.getId()) && existing.getPredecessorId() != null) {
                relations.put(existing.getId(), List.of(legacyDependency(existing.getPredecessorId())));
            }
        }
        relations.put(itemId, requestedDependencies);
        validateDependencyGraph(relations);

        // 2. Actualizamos la partida actual con las fechas que mandó Angular
        item.setStartDate(dto.getStartDate());
        item.setEndDate(dto.getEndDate());
        // Se conserva la columna anterior para que los cronogramas existentes sigan funcionando.
        item.setPredecessorId(requestedDependencies.isEmpty() ? null : requestedDependencies.get(0).getPredecessorId());

        dependencyRepository.deleteBySuccessorId(itemId);
        List<ProjectItemDependency> dependencies = requestedDependencies.stream().map(dependency -> {
            ProjectItemDependency relation = new ProjectItemDependency();
            relation.setSuccessor(item);
            relation.setPredecessor(itemMap.get(dependency.getPredecessorId()));
            relation.setType(dependency.getType() == null ? DependencyType.FINISH_TO_START : dependency.getType());
            return relation;
        }).collect(Collectors.toList());
        dependencyRepository.saveAll(dependencies);
        alignItemWithDependencies(item, requestedDependencies, itemMap);
        Set<Long> affected = new HashSet<>(Set.of(itemId));
        boolean discovered;
        do {
            discovered = false;
            for (ProjectItem successor : allProjectItems) {
                if (affected.contains(successor.getId())) continue;
                if (relations.getOrDefault(successor.getId(), List.of()).stream()
                        .anyMatch(link -> affected.contains(link.getPredecessorId()))) {
                    affected.add(successor.getId());
                    discovered = true;
                }
            }
        } while (discovered);
        for (int pass = 0; pass < affected.size(); pass++) {
            boolean moved = false;
            for (ProjectItem successor : allProjectItems) {
                if (successor.getId().equals(itemId) || !affected.contains(successor.getId())) continue;
                LocalDate before = successor.getStartDate();
                alignItemWithDependencies(successor, relations.getOrDefault(successor.getId(), List.of()), itemMap);
                if (!Objects.equals(before, successor.getStartDate())) moved = true;
            }
            if (!moved) break;
        }
        itemRepository.saveAll(affected.stream().map(itemMap::get).toList());

        // Registrar en el log de auditoría los cambios en el cronograma
        auditService.logAction("UPDATE", "Gantt", itemId, null, dto);
    }

    private void validatePredecessor(ProjectItem item, Long predecessorId, Map<Long, ProjectItem> itemMap) {
        if (predecessorId == null) {
            return;
        }
        if (item.getId().equals(predecessorId)) {
            throw new IllegalArgumentException("Una partida no puede depender de sí misma.");
        }

        ProjectItem predecessor = itemMap.get(predecessorId);
        if (predecessor == null) {
            throw new IllegalArgumentException("La partida predecesora no existe o no pertenece al mismo proyecto.");
        }

    }

    private GanttDependencyDto legacyDependency(Long predecessorId) {
        GanttDependencyDto dependency = new GanttDependencyDto();
        dependency.setPredecessorId(predecessorId);
        dependency.setType(DependencyType.FINISH_TO_START);
        return dependency;
    }

    private void validateDependencies(ProjectItem item, List<GanttDependencyDto> dependencies, Map<Long, ProjectItem> itemMap) {
        Set<Long> uniquePredecessors = new HashSet<>();
        for (GanttDependencyDto dependency : dependencies) {
            if (dependency == null || dependency.getPredecessorId() == null || !uniquePredecessors.add(dependency.getPredecessorId())) {
                throw new IllegalArgumentException("Cada relación debe tener una predecesora distinta.");
            }
            validatePredecessor(item, dependency.getPredecessorId(), itemMap);
        }
    }

    private void validateDependencyGraph(Map<Long, List<GanttDependencyDto>> relations) {
        Set<Long> visited = new HashSet<>();
        Set<Long> active = new HashSet<>();
        for (Long successor : relations.keySet()) {
            visitDependency(successor, relations, visited, active);
        }
    }

    private void visitDependency(Long id, Map<Long, List<GanttDependencyDto>> relations,
            Set<Long> visited, Set<Long> active) {
        if (visited.contains(id)) return;
        if (!active.add(id)) throw new IllegalArgumentException("La relación generaría un ciclo entre actividades.");
        for (GanttDependencyDto link : relations.getOrDefault(id, List.of())) {
            visitDependency(link.getPredecessorId(), relations, visited, active);
        }
        active.remove(id);
        visited.add(id);
    }

    /** Ajusta la actividad sucesora al hito que exige cada una de sus relaciones. */
    private void alignItemWithDependencies(ProjectItem item, List<GanttDependencyDto> dependencies, Map<Long, ProjectItem> itemMap) {
        if (item.getStartDate() == null || item.getEndDate() == null) return;
        LocalDate requiredStart = null;
        LocalDate requiredEnd = null;
        for (GanttDependencyDto dependency : dependencies) {
            ProjectItem predecessor = itemMap.get(dependency.getPredecessorId());
            if (predecessor == null || predecessor.getStartDate() == null || predecessor.getEndDate() == null) continue;
            DependencyType type = dependency.getType() == null ? DependencyType.FINISH_TO_START : dependency.getType();
            if (type == DependencyType.FINISH_TO_START) requiredStart = latest(requiredStart, predecessor.getEndDate());
            if (type == DependencyType.START_TO_START) requiredStart = latest(requiredStart, predecessor.getStartDate());
            if (type == DependencyType.FINISH_TO_FINISH) requiredEnd = latest(requiredEnd, predecessor.getEndDate());
            if (type == DependencyType.START_TO_FINISH) requiredEnd = latest(requiredEnd, predecessor.getStartDate());
        }
        LocalDate targetStart = requiredStart != null && item.getStartDate().isBefore(requiredStart) ? requiredStart : item.getStartDate();
        LocalDate targetEnd = requiredEnd != null && item.getEndDate().isBefore(requiredEnd) ? requiredEnd : item.getEndDate();
        long shift = Math.max(ChronoUnit.DAYS.between(item.getStartDate(), targetStart), ChronoUnit.DAYS.between(item.getEndDate(), targetEnd));
        if (shift > 0) {
            item.setStartDate(item.getStartDate().plusDays(shift));
            item.setEndDate(item.getEndDate().plusDays(shift));
        }
    }

    private LocalDate latest(LocalDate current, LocalDate candidate) {
        return current == null || candidate.isAfter(current) ? candidate : current;
    }

    private void cascadeDateShiftInMemory(Long parentId, long daysShifted, Map<Long, List<ProjectItem>> childrenGraph,
            List<ProjectItem> modifiedItems) {
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
        if (item == null)
            return;
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

    @Transactional
    public List<GanttItemResponseDto> getGanttItems(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        List<ProjectItem> items = itemRepository.findByProjectId(projectId);
        Map<Long, List<GanttDependencyDto>> dependenciesBySuccessor = dependencyRepository.findBySuccessorProjectId(projectId).stream()
                .collect(Collectors.groupingBy(relation -> relation.getSuccessor().getId(), Collectors.mapping(relation -> {
                    GanttDependencyDto dependency = new GanttDependencyDto();
                    dependency.setPredecessorId(relation.getPredecessor().getId());
                    dependency.setType(relation.getType());
                    return dependency;
                }, Collectors.toList())));
        // Respetamos el orden exacto del presupuesto
        items.sort(Comparator.comparing(ProjectItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProjectItem::getId));

        LocalDate currentDate = project.getStartDate();
        Long prevLeafItemId = null; // Guardará solo IDs de tareas "hijas"

        Set<Long> validIds = items.stream().map(ProjectItem::getId).collect(java.util.stream.Collectors.toSet());

        // 1. AUTO-LINKER: Solo para tareas reales (que tienen metrado)
        Map<Long, Long> predecessorRemap = new HashMap<>(); // Para reconectar dependencias "rotas" por inserciones
        List<ProjectItem> itemsToUpdate = new ArrayList<>();

        for (ProjectItem item : items) {
            boolean isParent = (item.getTotalQuantity() == null || item.getTotalQuantity() == 0);

            if (!isParent) {
                // 1. Si la partida ya tenía fechas, validamos si debemos "empujarla" (splice o
                // heal)
                if (item.getStartDate() != null) {
                    Long currentPredId = item.getPredecessorId();
                    boolean modified = false;

                    // A) HEALING DE BORRADO: Si su predecesor fue borrado, lo amarramos a la última
                    // hoja válida
                    if (currentPredId != null && !validIds.contains(currentPredId)) {
                        item.setPredecessorId(prevLeafItemId);
                        currentPredId = prevLeafItemId;
                        modified = true;
                    }

                    // B) HEALING DE INSERCIÓN: Si su predecesor fue reemplazado, actualizamos su
                    // flecha
                    if (currentPredId != null && predecessorRemap.containsKey(currentPredId)) {
                        Long newPredId = predecessorRemap.get(currentPredId);
                        if (!currentPredId.equals(newPredId)) {
                            item.setPredecessorId(newPredId);
                            modified = true;
                        }
                    }

                    if (modified) {
                        itemsToUpdate.add(item);
                    }
                }

                // 2. Si la partida es NUEVA (sin fechas)
                if (item.getStartDate() == null) {
                    int baseDays = 1;
                    if (item.getLaborYield() != null && item.getLaborYield() > 0) {
                        baseDays = (int) Math.ceil(item.getTotalQuantity() / item.getLaborYield());
                    }

                    item.setStartDate(currentDate);
                    item.setEndDate(currentDate.plusDays(baseDays));
                    item.setPredecessorId(prevLeafItemId); // Se amarra a la hija anterior
                    itemsToUpdate.add(item);

                    // Reemplazamos al antiguo líder (prevLeafItemId) con este nuevo ítem
                    if (prevLeafItemId != null) {
                        predecessorRemap.put(prevLeafItemId, item.getId());
                    }
                    // Si algo ya había sido redirigido a prevLeafItemId, lo redirigimos a este
                    // nuevo también
                    for (Map.Entry<Long, Long> entry : predecessorRemap.entrySet()) {
                        if (entry.getValue().equals(prevLeafItemId)) {
                            entry.setValue(item.getId());
                        }
                    }
                }

                if (item.getEndDate() != null) {
                    currentDate = item.getEndDate();
                }
                prevLeafItemId = item.getId();
            }
        }

        if (!itemsToUpdate.isEmpty()) {
            itemRepository.saveAll(itemsToUpdate);
        }

        // --- PRE-FETCH EN BATCH DE METRADOS EJECUTADOS (ELIMINA N+1) ---
        List<Object[]> executedResults = reportRepository.sumExecutedQuantityByProjectIdGroupedByItemId(projectId);
        Map<Long, Double> executedMap = new HashMap<>();
        for (Object[] result : executedResults) {
            executedMap.put((Long) result[0], ((Number) result[1]).doubleValue());
        }

        // 2. MAPEO CON JERARQUÍA (Padres e Hijos)
        Map<Integer, Long> levelTracker = new HashMap<>(); // Para rastrear quién es el padre actual de cada nivel

        return items.stream().map(item -> {
            int currentLevel = item.getLevel() != null ? item.getLevel() : 0;
            levelTracker.put(currentLevel, item.getId()); // Actualizamos el ID de este nivel

            // El padre siempre es el último elemento registrado en el nivel superior
            // (currentLevel - 1)
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
            List<GanttDependencyDto> dependencies = dependenciesBySuccessor.getOrDefault(item.getId(), new ArrayList<>());
            if (dependencies.isEmpty() && item.getPredecessorId() != null) dependencies = List.of(legacyDependency(item.getPredecessorId()));
            dto.setDependencies(dependencies);
            dto.setCode(item.getCode());
            // --- INYECCIÓN DE JERARQUÍA ---
            dto.setParentId(myParentId);
            dto.setLevel(currentLevel);
            dto.setType(isParent ? "project" : "task");
            // ------------------------------

            int baseDays = 1;
            if (!isParent && item.getTotalQuantity() != null && item.getLaborYield() != null
                    && item.getLaborYield() > 0) {
                baseDays = (int) Math.ceil(item.getTotalQuantity() / item.getLaborYield());
            }
            dto.setBaseDurationDays(isParent ? 0 : baseDays);

            Double executed = executedMap.get(item.getId());
            double progress = 0.0;
            if (!isParent && executed != null && item.getTotalQuantity() > 0) {
                progress = (executed / item.getTotalQuantity()) * 100;
            }
            dto.setCurrentProgressPercentage(progress);

            return dto;
        }).collect(Collectors.toList());
    }
}
