package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectItemResourceRequestDto {
    @NotNull(message = "El ID del recurso es obligatorio")
    private Long resourceId;

    // Puede ser nulo si es un material
    private Double squad;

    // Será obligatorio si es material, y opcional si es MO/Equipo (porque lo calcularemos)
    private Double quantity;
}