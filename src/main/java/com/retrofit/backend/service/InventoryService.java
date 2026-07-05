package com.retrofit.backend.service;
import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
import com.retrofit.backend.dto.PlannedResourceDTO;
import com.retrofit.backend.dto.StockSummaryDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.retrofit.backend.dto.SupplyControlDTO;
import java.util.List;

public interface InventoryService {
    InventoryTransactionResponseDTO registerInbound(InventoryTransactionRequestDTO request);
    InventoryTransactionResponseDTO registerOutbound(InventoryTransactionRequestDTO request);
    BigDecimal getCurrentStock(Long projectId, Long resourceId);
    Page<StockSummaryDTO> getProjectStockSummary(Long projectId, String search, Pageable pageable);
    List<InventoryTransactionResponseDTO> getKardex(Long projectId, Long resourceId);
    List<PlannedResourceDTO> getPlannedMaterialsForProject(Long projectId);
    List<SupplyControlDTO> getSupplyControl(Long projectId, String status, String resourceName);
    BigDecimal getConsumedQuantity(Long projectItemId, Long resourceId, LocalDate date);
}
