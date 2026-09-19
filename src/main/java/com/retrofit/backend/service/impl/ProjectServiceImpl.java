package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.ProjectRequestDto;
import com.retrofit.backend.dto.ProjectResponseDto;
import com.retrofit.backend.exceptions.ResourceNotFoundException;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.model.ProjectItemResource;
import com.retrofit.backend.model.Resource;
import com.retrofit.backend.enums.ProjectPriority;
import com.retrofit.backend.enums.ProjectStatus;
import com.retrofit.backend.model.User;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.ResourceRepository;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ProjectItemRepository itemRepository;
    private final ResourceRepository resourceRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<ProjectResponseDto> getAllProjects(String search, String priorityStr, String statusStr, Pageable pageable) {

        ProjectPriority priority = null;
        if (priorityStr != null && !priorityStr.trim().isEmpty()) {
            try {
                priority = ProjectPriority.valueOf(priorityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignorado
            }
        }

        ProjectStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = ProjectStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignorado
            }
        }


        String finalSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : "";

        return projectRepository.findWithFilters(finalSearch, priority, status, pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Proyectos")
    public ProjectResponseDto createProject(ProjectRequestDto dto) {
        if(projectRepository.existsByCode(dto.getCode())) {
            throw new com.retrofit.backend.exceptions.DuplicateResourceException("code", "Este código de proyecto ya está registrado");
        }
        Project project = new Project();
        return saveProjectFromDto(project, dto);
    }

    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Proyectos")
    public ProjectResponseDto duplicateProject(Long sourceProjectId, ProjectRequestDto dto) {
        Project sourceProject = projectRepository.findById(sourceProjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto origen no encontrado"));

        if (projectRepository.existsByCode(dto.getCode())) {
            throw new com.retrofit.backend.exceptions.DuplicateResourceException("code", "Este código de proyecto ya está registrado");
        }

        Project newProject = new Project();
        newProject.setCode(dto.getCode());
        newProject.setName(dto.getName());
        newProject.setClient(dto.getClient());
        newProject.setLocation(dto.getLocation());
        newProject.setDescription(dto.getDescription());
        newProject.setStartDate(dto.getStartDate());

        try {
            newProject.setStatus(ProjectStatus.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.retrofit.backend.exceptions.InvalidEnumException("status", "El estado seleccionado no es válido");
        }

        try {
            newProject.setPriority(ProjectPriority.valueOf(dto.getPriority().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.retrofit.backend.exceptions.InvalidEnumException("priority", "La prioridad seleccionada no es válida");
        }

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró al responsable con el ID proporcionado"));
            newProject.setManager(manager);
        }

        newProject.setCurrentProgress(0.0);
        newProject.setGeneralExpensesPercentage(
                sourceProject.getGeneralExpensesPercentage() != null ? sourceProject.getGeneralExpensesPercentage() : 5.0);
        newProject.setUtilityPercentage(
                sourceProject.getUtilityPercentage() != null ? sourceProject.getUtilityPercentage() : 4.0);
        newProject.setTotalBudget(0.0);

        newProject = projectRepository.save(newProject);

        List<ProjectItem> sourceItems = itemRepository.findByProjectIdOrderByItemOrderAsc(sourceProjectId);

        if (sourceItems.isEmpty()) {
            return convertToDto(newProject);
        }

        // Bulk Pre-fetching: Cargar todos los recursos requeridos en 1 único viaje a la BD (elimina N+1 SELECTs)
        Set<Long> resourceIds = sourceItems.stream()
                .filter(item -> item.getApuDetails() != null)
                .flatMap(item -> item.getApuDetails().stream())
                .filter(apu -> apu.getResource() != null)
                .map(apu -> apu.getResource().getId())
                .collect(Collectors.toSet());

        Map<Long, Resource> resourceMap = resourceIds.isEmpty() ? Collections.emptyMap() :
                resourceRepository.findAllById(resourceIds).stream()
                        .collect(Collectors.toMap(Resource::getId, r -> r));

        long dayDiff = 0;
        if (sourceProject.getStartDate() != null && newProject.getStartDate() != null) {
            dayDiff = ChronoUnit.DAYS.between(sourceProject.getStartDate(), newProject.getStartDate());
        }

        double calculatedDirectCost = 0.0;
        List<Double> itemUnitPrices = new ArrayList<>();
        List<List<PreparedApu>> itemApuLists = new ArrayList<>();

        for (ProjectItem sourceItem : sourceItems) {
            double calculatedUnitPrice = 0.0;
            List<PreparedApu> apuList = new ArrayList<>();

            if (sourceItem.getApuDetails() != null && !sourceItem.getApuDetails().isEmpty()) {
                for (ProjectItemResource sourceApu : sourceItem.getApuDetails()) {
                    Resource currentResource = resourceMap.getOrDefault(
                            sourceApu.getResource().getId(), sourceApu.getResource());

                    PreparedApu apu = new PreparedApu();
                    apu.resourceId = currentResource.getId();
                    apu.squad = sourceApu.getSquad() != null ? sourceApu.getSquad() : 0.0;
                    apu.quantity = sourceApu.getQuantity() != null ? sourceApu.getQuantity() : 0.0;

                    double basePrice = currentResource.getBasePrice() != null ? currentResource.getBasePrice() : 0.0;
                    double partialPrice = Math.round(apu.quantity * basePrice * 100.0) / 100.0;
                    apu.partialPrice = partialPrice;

                    calculatedUnitPrice += partialPrice;
                    apuList.add(apu);
                }
            } else {
                calculatedUnitPrice = sourceItem.getUnitPrice() != null ? sourceItem.getUnitPrice() : 0.0;
            }

            itemUnitPrices.add(calculatedUnitPrice);
            itemApuLists.add(apuList);

            if (sourceItem.getTotalQuantity() != null && calculatedUnitPrice > 0) {
                calculatedDirectCost += (sourceItem.getTotalQuantity() * calculatedUnitPrice);
            }
        }

        // Inserción multi-fila masiva de partidas en 1 solo viaje a la BD con RETURNING id, item_order
        StringBuilder sql = new StringBuilder(
                "INSERT INTO project_items (project_id, code, description, unit, total_quantity, indent_level, item_order, labor_yield, equipment_yield, start_date, end_date, unit_price, created_at) VALUES "
        );
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < sourceItems.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())");
            ProjectItem si = sourceItems.get(i);
            params.add(newProject.getId());
            params.add(si.getCode());
            params.add(si.getDescription());
            params.add(si.getUnit());
            params.add(si.getTotalQuantity());
            params.add(si.getLevel());
            params.add(si.getItemOrder() != null ? si.getItemOrder() : i);
            params.add(si.getLaborYield() != null ? si.getLaborYield() : 0.0);
            params.add(si.getEquipmentYield() != null ? si.getEquipmentYield() : 0.0);
            params.add(si.getStartDate() != null ? si.getStartDate().plusDays(dayDiff) : null);
            params.add(si.getEndDate() != null ? si.getEndDate().plusDays(dayDiff) : null);
            params.add(itemUnitPrices.get(i));
        }
        sql.append(" RETURNING id, item_order");

        List<Map<String, Object>> insertedRows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        Map<Integer, Long> orderToNewIdMap = new HashMap<>();
        for (Map<String, Object> row : insertedRows) {
            Long newId = ((Number) row.get("id")).longValue();
            Integer order = ((Number) row.get("item_order")).intValue();
            orderToNewIdMap.put(order, newId);
        }

        Map<Long, Long> oldIdToNewIdMap = new HashMap<>();
        List<PreparedApu> allApusToInsert = new ArrayList<>();

        for (int i = 0; i < sourceItems.size(); i++) {
            ProjectItem sourceItem = sourceItems.get(i);
            int order = sourceItem.getItemOrder() != null ? sourceItem.getItemOrder() : i;
            Long newItemId = orderToNewIdMap.get(order);
            if (newItemId != null) {
                oldIdToNewIdMap.put(sourceItem.getId(), newItemId);
                for (PreparedApu apu : itemApuLists.get(i)) {
                    apu.newItemId = newItemId;
                    allApusToInsert.add(apu);
                }
            }
        }

        // Inserción en lote masivo de todos los APUs en 1 solo viaje a la BD
        if (!allApusToInsert.isEmpty()) {
            String apuInsertSql = "INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.batchUpdate(apuInsertSql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int idx) throws java.sql.SQLException {
                    PreparedApu a = allApusToInsert.get(idx);
                    ps.setLong(1, a.newItemId);
                    ps.setLong(2, a.resourceId);
                    ps.setDouble(3, a.squad);
                    ps.setDouble(4, a.quantity);
                    ps.setDouble(5, a.partialPrice);
                }
                @Override
                public int getBatchSize() {
                    return allApusToInsert.size();
                }
            });
        }

        // Actualización selectiva de predecesores en lote
        List<long[]> predecessorsToUpdate = new ArrayList<>();
        for (ProjectItem sourceItem : sourceItems) {
            if (sourceItem.getPredecessorId() != null) {
                Long newPredId = oldIdToNewIdMap.get(sourceItem.getPredecessorId());
                Long newItemId = oldIdToNewIdMap.get(sourceItem.getId());
                if (newPredId != null && newItemId != null) {
                    predecessorsToUpdate.add(new long[]{newPredId, newItemId});
                }
            }
        }

        if (!predecessorsToUpdate.isEmpty()) {
            String predUpdateSql = "UPDATE project_items SET predecessor_id = ? WHERE id = ?";
            jdbcTemplate.batchUpdate(predUpdateSql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int idx) throws java.sql.SQLException {
                    long[] pair = predecessorsToUpdate.get(idx);
                    ps.setLong(1, pair[0]);
                    ps.setLong(2, pair[1]);
                }
                @Override
                public int getBatchSize() {
                    return predecessorsToUpdate.size();
                }
            });
        }

        double gePercent = newProject.getGeneralExpensesPercentage();
        double utPercent = newProject.getUtilityPercentage();
        double generalExpenses = calculatedDirectCost * (gePercent / 100.0);
        double utility = calculatedDirectCost * (utPercent / 100.0);
        double subtotal = calculatedDirectCost + generalExpenses + utility;
        double igv = subtotal * 0.18;
        newProject.setTotalBudget(subtotal + igv);
        projectRepository.save(newProject);

        return convertToDto(newProject);
    }

    private static class PreparedApu {
        Long newItemId;
        Long resourceId;
        double squad;
        double quantity;
        double partialPrice;
    }

    @Override
    @Transactional
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto dto) {
        // 1. Buscamos el proyecto a actualizar
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectResponseDto estadoAnterior = convertToDto(project);

        // 2. Validamos que si el usuario cambió el código, el nuevo código no le pertenezca a OTRO proyecto
        projectRepository.findByCode(dto.getCode())
                .ifPresent(existingProject -> {
                    if (!existingProject.getId().equals(id)) {
                        throw new com.retrofit.backend.exceptions.DuplicateResourceException("code", "Este código de proyecto ya está registrado");
                    }
                });

        // 3. Guardamos los datos primero para obtener el estado nuevo real
        ProjectResponseDto estadoNuevo = saveProjectFromDto(project, dto);
        auditService.logAction("UPDATE", "Proyectos", project.getId(), estadoAnterior, estadoNuevo);
        return estadoNuevo;
    }

    @Override
    @Transactional
    @AuditChange(action = "DELETE", module = "Proyectos")
    public void deleteProject(Long id) {
        // 1. Verificación atómica en un único round-trip: existencia y registros operativos vinculados
        Map<String, Object> counts = jdbcTemplate.queryForMap(
                "SELECT " +
                "  (SELECT COUNT(*) FROM projects WHERE id = ?) AS project_count, " +
                "  (SELECT COUNT(*) FROM progress_reports pr JOIN project_items pi ON pr.project_item_id = pi.id WHERE pi.project_id = ?) AS progress_count, " +
                "  (SELECT COUNT(*) FROM inventory_transactions WHERE project_id = ?) AS inventory_count, " +
                "  (SELECT COUNT(*) FROM project_assignments WHERE project_id = ?) AS assignment_count",
                id, id, id, id);

        long projectCount = ((Number) counts.get("project_count")).longValue();
        if (projectCount == 0) {
            throw new ResourceNotFoundException("Project not found");
        }

        long progressCount = ((Number) counts.get("progress_count")).longValue();
        long inventoryCount = ((Number) counts.get("inventory_count")).longValue();
        long assignmentCount = ((Number) counts.get("assignment_count")).longValue();

        if (progressCount > 0 || inventoryCount > 0 || assignmentCount > 0) {
            List<String> reasons = new ArrayList<>();
            if (progressCount > 0) {
                reasons.add(progressCount + (progressCount == 1 ? " reporte de avance" : " reportes de avance"));
            }
            if (inventoryCount > 0) {
                reasons.add(inventoryCount + (inventoryCount == 1 ? " movimiento en almacén/kardex" : " movimientos en almacén/kardex"));
            }
            if (assignmentCount > 0) {
                reasons.add(assignmentCount + (assignmentCount == 1 ? " asignación de personal" : " asignaciones de personal"));
            }
            throw new IllegalStateException("No se puede eliminar el proyecto porque cuenta con registros operativos activos: "
                    + String.join(", ", reasons)
                    + ". Para salvaguardar la trazabilidad histórica y auditoría, los proyectos con actividad no pueden eliminarse.");
        }

        // 2. Eliminación atómica limpia en un único viaje de red mediante CTE (proyecto sin actividad operativa)
        jdbcTemplate.update(
                "WITH del_apu AS (" +
                "  DELETE FROM project_item_resources WHERE project_item_id IN (" +
                "    SELECT id FROM project_items WHERE project_id = ?" +
                "  )" +
                "), " +
                "del_items AS (" +
                "  DELETE FROM project_items WHERE project_id = ?" +
                ") " +
                "DELETE FROM projects WHERE id = ?",
                id, id, id);
    }

    @Override
    public ProjectResponseDto getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    @Override
    public ProjectResponseDto getProjectByCode(String code) {
        return projectRepository.findByCode(code)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectResponseDto saveProjectFromDto(Project project, ProjectRequestDto dto) {
        project.setCode(dto.getCode());
        project.setName(dto.getName());
        project.setClient(dto.getClient());
        project.setLocation(dto.getLocation());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());

        try {
            project.setStatus(ProjectStatus.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.retrofit.backend.exceptions.InvalidEnumException("status", "El estado seleccionado no es válido");
        }

        try {
            project.setPriority(ProjectPriority.valueOf(dto.getPriority().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.retrofit.backend.exceptions.InvalidEnumException("priority", "La prioridad seleccionada no es válida");
        }

        if(dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró al responsable con el ID proporcionado"));
            project.setManager(manager);
        }

        return convertToDto(projectRepository.save(project));
    }

    private ProjectResponseDto convertToDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();
        dto.setId(project.getId());
        dto.setCode(project.getCode());
        dto.setName(project.getName());
        dto.setClient(project.getClient());

        dto.setLocation(project.getLocation());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());

        dto.setStatus(project.getStatus().name());
        dto.setPriority(project.getPriority().name());
        dto.setCurrentProgress(project.getCurrentProgress());
        dto.setTotalBudget(project.getTotalBudget());
        dto.setGeneralExpensesPercentage(project.getGeneralExpensesPercentage());
        dto.setUtilityPercentage(project.getUtilityPercentage());
        if(project.getManager() != null) {
            dto.setManagerId(project.getManager().getId());
            dto.setManagerFullName(project.getManager().getName() + " " + project.getManager().getLastName());
        }
        return dto;
    }
}
