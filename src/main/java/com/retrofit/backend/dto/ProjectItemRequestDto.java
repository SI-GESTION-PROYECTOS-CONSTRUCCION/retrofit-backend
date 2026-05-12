package com.retrofit.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProjectItemRequestDto {
    private String code;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    private String unit;
    private Double totalQuantity;
    private Double unitPrice;

    private Integer level;
}