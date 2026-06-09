package com.retrofit.backend.controller;

import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
import com.retrofit.backend.dto.PlannedResourceDTO;
import com.retrofit.backend.dto.StockSummaryDTO;
import com.retrofit.backend.dto.SupplyControlDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.retrofit.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 1. Registrar una ENTRADA de material al almacén
    @PostMapping("/inbound")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public ResponseEntity<InventoryTransactionResponseDTO> registerInbound(@RequestBody InventoryTransactionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.registerInbound(request));
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public ResponseEntity<InventoryTransactionResponseDTO> registerOutbound(@RequestBody InventoryTransactionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.registerOutbound(request));
    }

    // 3. Consultar el STOCK ACTUAL de un material específico
    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<BigDecimal> getCurrentStock(
            @RequestParam Long projectId,
            @RequestParam Long resourceId) {
        BigDecimal currentStock = inventoryService.getCurrentStock(projectId, resourceId);
        return ResponseEntity.ok(currentStock);
    }

    // 4. Obtener el KARDEX (Historial de movimientos)
    @GetMapping("/kardex")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<List<InventoryTransactionResponseDTO>> getKardex(
            @RequestParam Long projectId,
            @RequestParam Long resourceId) {
        return ResponseEntity.ok(inventoryService.getKardex(projectId, resourceId));
    }

    // 5. Obtener el RESUMEN DE INVENTARIO (Todos los materiales de una obra)
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<Page<StockSummaryDTO>> getProjectStockSummary(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getProjectStockSummary(projectId, PageRequest.of(page, size)));
    }

    @GetMapping("/planned-materials")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<List<PlannedResourceDTO>> getPlannedMaterials(
            @RequestParam Long projectId) {
        return ResponseEntity.ok(inventoryService.getPlannedMaterialsForProject(projectId));
    }

    // 7. Obtener la vista gerencial de Control de Abastecimiento (Explosion vs Fisico)
    @GetMapping("/supply-control")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<List<SupplyControlDTO>> getSupplyControl(
            @RequestParam Long projectId) {
        return ResponseEntity.ok(inventoryService.getSupplyControl(projectId));
    }

    // 8. Obtener cantidad consumida por partida (Para automatizacion de avance diario)
    @GetMapping("/consumed-quantity")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<BigDecimal> getConsumedQuantity(
            @RequestParam Long projectItemId,
            @RequestParam Long resourceId) {
        return ResponseEntity.ok(inventoryService.getConsumedQuantity(projectItemId, resourceId));
    }
}