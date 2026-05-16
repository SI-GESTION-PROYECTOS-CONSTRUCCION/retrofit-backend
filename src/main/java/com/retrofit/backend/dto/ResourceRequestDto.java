package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ResourceRequestDto {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "La unidad es obligatoria")
    private String unit;

    @NotNull(message = "El precio base es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Double basePrice;
}