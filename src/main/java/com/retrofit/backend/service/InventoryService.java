package com.retrofit.backend.service;
import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
import com.retrofit.backend.dto.PlannedResourceDTO;
import com.retrofit.backend.dto.StockSummaryDTO;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    InventoryTransactionResponseDTO registerInbound(InventoryTransactionRequestDTO request);
    InventoryTransactionResponseDTO registerOutbound(InventoryTransactionRequestDTO request);
    BigDecimal getCurrentStock(Long projectId, Long resourceId);
    List<StockSummaryDTO> getProjectStockSummary(Long projectId);
    List<InventoryTransactionResponseDTO> getKardex(Long projectId, Long resourceId);
    List<PlannedResourceDTO> getPlannedMaterialsForProject(Long projectId);
}
