package com.retrofit.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProjectItemRequestDto {
    private Long id;
    private String code;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    private String unit;
    private Double totalQuantity;
    private Double unitPrice;
    private Integer itemOrder;
    private Integer level;
    private Double laborYield;
    private Double equipmentYield;
}