package com.retrofit.backend.dto;

import com.retrofit.backend.enums.TransactionReason;
import com.retrofit.backend.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionResponseDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long resourceId;
    private String resourceName;
    private String resourceUnit;
    private Long projectItemId;
    private String projectItemCode;
    private String projectItemDescription;
    private TransactionType transactionType;
    private TransactionReason reason;
    private BigDecimal quantity;
    private String referenceDocument;
    private LocalDateTime transactionDate;
    private String createdBy;
    private String observations;
}