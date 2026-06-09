package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyControlDTO {
    private Long resourceId;
    private String resourceName;
    private String resourceUnit;
    private BigDecimal budgetedQuantity; // Explosión de Insumos
    private BigDecimal receivedQuantity; // Ingresos físicos al almacén
    private BigDecimal missingQuantity;  // budgetedQuantity - receivedQuantity
    private String status;               // OK, PENDING, EXCESS
}
