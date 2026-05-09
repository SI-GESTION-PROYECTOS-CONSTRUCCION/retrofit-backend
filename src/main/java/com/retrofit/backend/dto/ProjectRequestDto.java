package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.time.LocalDate;

@Data
public class ProjectRequestDto {
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código no puede exceder los 20 caracteres")
    private String code;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(min = 5, max = 150, message = "El nombre debe tener entre 5 y 150 caracteres")
    private String name;

    @NotBlank(message = "El cliente es obligatorio")
    private String client;

    private String location;

    private String description;

    @NotNull(message = "La fecha estimada de entrega es obligatoria")
    @FutureOrPresent(message = "La fecha estimada de entrega debe ser hoy o en el futuro")
    private LocalDate estimatedDeliveryDate;

    @NotBlank(message = "El estado inicial es obligatorio")
    private String status;

    @NotBlank(message = "La prioridad es obligatoria")
    private String priority;

    @NotNull(message = "Debe asignar un responsable al proyecto")
    private Long managerId;
}
