package com.retrofit.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProgressReportRequestDto {
    @NotNull(message = "El ID de la partida (Project Item) es obligatorio")
    private Long projectItemId;

    @NotNull(message = "La fecha del reporte es obligatoria")
    @PastOrPresent(message = "No se pueden registrar avances en fechas futuras")
    private LocalDate reportDate;

    @NotNull(message = "El metrado ejecutado es obligatorio")
    @Positive(message = "El metrado ejecutado debe ser mayor a cero")
    private Double executedQuantity;

    private String observations;
}