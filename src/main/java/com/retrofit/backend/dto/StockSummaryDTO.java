package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockSummaryDTO {
    private Long projectId;
    private Long resourceId;
    private String resourceName;
    private String resourceUnit;
    private BigDecimal currentStock;
}
