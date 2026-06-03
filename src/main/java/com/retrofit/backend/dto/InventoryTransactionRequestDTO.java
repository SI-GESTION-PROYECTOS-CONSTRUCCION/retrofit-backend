package com.retrofit.backend.dto;

import com.retrofit.backend.enums.TransactionReason;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryTransactionRequestDTO {
    private Long projectId;
    private Long resourceId;
    private Long projectItemId;
    private TransactionReason reason;
    private BigDecimal quantity;
    private String referenceDocument;
    private String observations;
    private String createdBy;
    private LocalDateTime transactionDate;
}