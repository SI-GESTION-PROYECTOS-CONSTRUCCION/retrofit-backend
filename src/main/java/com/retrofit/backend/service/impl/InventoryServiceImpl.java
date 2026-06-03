    package com.retrofit.backend.service.impl;

    import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
    import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
    import com.retrofit.backend.dto.PlannedResourceDTO;
    import com.retrofit.backend.dto.StockSummaryDTO;
    import com.retrofit.backend.enums.TransactionType;
    import com.retrofit.backend.model.InventoryTransaction;
    import com.retrofit.backend.model.Project;
    import com.retrofit.backend.model.ProjectItem;
    import com.retrofit.backend.model.Resource;
    import com.retrofit.backend.repository.*;
    import com.retrofit.backend.service.InventoryService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
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
        public InventoryTransactionResponseDTO registerInbound(InventoryTransactionRequestDTO request) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new RuntimeException("Recurso no encontrado"));

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .project(project)
                    .resource(resource)
                    .transactionType(TransactionType.INBOUND)
                    .reason(request.getReason())
                    .quantity(request.getQuantity())
                    .referenceDocument(request.getReferenceDocument())
                    .observations(request.getObservations())
                    .createdBy(request.getCreatedBy())
                    .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                    .build();

            InventoryTransaction savedTransaction = transactionRepository.save(transaction);
            return mapToResponseDTO(savedTransaction);
        }

        @Override
        @Transactional
        public InventoryTransactionResponseDTO registerOutbound(InventoryTransactionRequestDTO request) {
            // 1. Validar que la Partida venga en el request (Obligatorio para salidas)
            if (request.getProjectItemId() == null) {
                throw new IllegalArgumentException("Para registrar una salida de material, debe especificar la Partida destino.");
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

            // 3. Registrar la salida
            InventoryTransaction transaction = InventoryTransaction.builder()
                    .project(project)
                    .resource(resource)
                    .projectItem(projectItem)
                    .transactionType(TransactionType.OUTBOUND)
                    .reason(request.getReason())
                    .quantity(request.getQuantity())
                    .referenceDocument(request.getReferenceDocument())
                    .observations(request.getObservations())
                    .createdBy(request.getCreatedBy())
                    .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
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
        public List<StockSummaryDTO> getProjectStockSummary(Long projectId) {
            return transactionRepository.getProjectStockSummary(projectId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<PlannedResourceDTO> getPlannedMaterialsForProject(Long projectId) {
            return projectItemResourceRepository.findPlannedMaterialsByProjectId(projectId);
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
                    .projectItemCode(transaction.getProjectItem() != null ? transaction.getProjectItem().getCode() : null) // Asumiendo getItemCode()
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
