package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
import com.retrofit.backend.dto.PlannedResourceDTO;
import com.retrofit.backend.dto.StockSummaryDTO;
import com.retrofit.backend.dto.SupplyControlDTO;
import com.retrofit.backend.enums.TransactionType;
import com.retrofit.backend.model.InventoryTransaction;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.ProjectItem;
import com.retrofit.backend.model.Resource;
import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.repository.*;
import com.retrofit.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryTransactionRepository transactionRepository;
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final ProjectItemRepository projectItemRepository;
    private final ProjectItemResourceRepository projectItemResourceRepository;

    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Inventario")
    public InventoryTransactionResponseDTO registerInbound(InventoryTransactionRequestDTO request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado"));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        InventoryTransaction transaction = InventoryTransaction.builder()
                .project(project)
                .resource(resource)
                .transactionType(TransactionType.INBOUND)
                .reason(request.getReason())
                .quantity(request.getQuantity())
                .referenceDocument(request.getReferenceDocument())
                .observations(request.getObservations())
                .createdBy(currentUsername)
                .transactionDate(
                        request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .build();

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponseDTO(savedTransaction);
    }

    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Inventario")
    public InventoryTransactionResponseDTO registerOutbound(InventoryTransactionRequestDTO request) {
        // 1. Validar que la Partida venga en el request (Obligatorio para salidas)
        if (request.getProjectItemId() == null) {
            throw new IllegalArgumentException(
                    "Para registrar una salida de material, debe especificar la Partida destino.");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado"));

        ProjectItem projectItem = projectItemRepository.findById(request.getProjectItemId())
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        // 2. REGLA DE NEGOCIO: Validar que haya stock suficiente
        BigDecimal currentStock = getCurrentStock(request.getProjectId(), request.getResourceId());
        if (currentStock.compareTo(request.getQuantity()) < 0) {
            throw new RuntimeException("Stock insuficiente. Stock actual: " + currentStock + " " + resource.getUnit() +
                    ", Cantidad solicitada: " + request.getQuantity());
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        InventoryTransaction transaction = InventoryTransaction.builder()
                .project(project)
                .resource(resource)
                .projectItem(projectItem)
                .transactionType(TransactionType.OUTBOUND)
                .reason(request.getReason())
                .quantity(request.getQuantity())
                .referenceDocument(request.getReferenceDocument())
                .observations(request.getObservations())
                .createdBy(currentUsername)
                .transactionDate(
                        request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .build();

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponseDTO(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentStock(Long projectId, Long resourceId) {
        return transactionRepository.calculateCurrentStock(projectId, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockSummaryDTO> getProjectStockSummary(Long projectId, String search, Pageable pageable) {
        String safeSearch = search == null ? "" : search;
        return transactionRepository.getProjectStockSummary(projectId, safeSearch, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlannedResourceDTO> getPlannedMaterialsForProject(Long projectId) {
        return projectItemResourceRepository.findPlannedMaterialsByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplyControlDTO> getSupplyControl(Long projectId, String statusFilter, String resourceNameFilter) {
        // 1. Obtener Explosión de Insumos (Budgeted Quantity)
        List<Object[]> budgetedList = projectItemResourceRepository.getBudgetedQuantitiesByProject(projectId);

        // 2. Obtener Ingresos a Almacén (Received Quantity)
        List<Object[]> receivedList = transactionRepository.getReceivedQuantityByProject(projectId);

        // 3. Mapear recibidos para búsqueda rápida
        java.util.Map<Long, BigDecimal> receivedMap = new java.util.HashMap<>();
        for (Object[] r : receivedList) {
            Long resId = (Long) r[0];
            BigDecimal recQty = (BigDecimal) r[1];
            receivedMap.put(resId, recQty != null ? recQty : BigDecimal.ZERO);
        }

        // 4. Construir resultado
        List<SupplyControlDTO> result = new java.util.ArrayList<>();
        for (Object[] b : budgetedList) {
            Long resId = (Long) b[0];
            String resName = (String) b[1];
            String resUnit = (String) b[2];
            Double budgQtyDouble = (Double) b[3];
            BigDecimal budgQty = budgQtyDouble != null ? BigDecimal.valueOf(budgQtyDouble) : BigDecimal.ZERO;

            BigDecimal recQty = receivedMap.getOrDefault(resId, BigDecimal.ZERO);
            BigDecimal missingQty = budgQty.subtract(recQty);

            String status = "OK";
            if (missingQty.compareTo(BigDecimal.ZERO) > 0) {
                status = "PENDING";
            } else if (missingQty.compareTo(BigDecimal.ZERO) < 0) {
                status = "EXCESS";
            }

            boolean matchName = resourceNameFilter == null || resourceNameFilter.trim().isEmpty() ||
                    (resName != null && resName.toLowerCase().contains(resourceNameFilter.toLowerCase().trim()));
            boolean matchStatus = statusFilter == null || statusFilter.trim().isEmpty()
                    || statusFilter.equalsIgnoreCase(status);

            if (matchName && matchStatus) {
                result.add(new SupplyControlDTO(resId, resName, resUnit, budgQty, recQty, missingQty, status));
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getConsumedQuantity(Long projectItemId, Long resourceId, LocalDate date) {
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return transactionRepository.calculateConsumedQuantityByProjectItemAndDate(projectItemId, resourceId, start,
                    end);
        }
        return transactionRepository.calculateConsumedQuantityByProjectItem(projectItemId, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransactionResponseDTO> getKardex(Long projectId, Long resourceId) {
        return transactionRepository.findByProjectIdAndResourceIdOrderByTransactionDateDesc(projectId, resourceId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    private InventoryTransactionResponseDTO mapToResponseDTO(InventoryTransaction transaction) {
        return InventoryTransactionResponseDTO.builder()
                .id(transaction.getId())
                .projectId(transaction.getProject().getId())
                .projectName(transaction.getProject().getName())
                .resourceId(transaction.getResource().getId())
                .resourceName(transaction.getResource().getName())
                .resourceUnit(transaction.getResource().getUnit())
                .projectItemId(transaction.getProjectItem() != null ? transaction.getProjectItem().getId() : null)
                .projectItemCode(transaction.getProjectItem() != null ? transaction.getProjectItem().getCode() : null)
                .projectItemDescription(
                        transaction.getProjectItem() != null ? transaction.getProjectItem().getDescription() : null)
                .transactionType(transaction.getTransactionType())
                .reason(transaction.getReason())
                .quantity(transaction.getQuantity())
                .referenceDocument(transaction.getReferenceDocument())
                .transactionDate(transaction.getTransactionDate())
                .createdBy(transaction.getCreatedBy())
                .observations(transaction.getObservations())
                .build();
    }
}
