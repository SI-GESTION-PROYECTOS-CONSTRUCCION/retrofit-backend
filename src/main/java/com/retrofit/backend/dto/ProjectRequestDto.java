package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.time.LocalDate;

@Data
public class ProjectRequestDto {
    public interface OnCreate {}

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código no puede exceder los 20 caracteres")
    private String code;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(min = 5, max = 150, message = "El nombre debe tener entre 5 y 150 caracteres")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[a-zA-ZñÑáéíóúÁÉÍÓÚ])[a-zA-ZñÑáéíóúÁÉÍÓÚ0-9 ]+$", message = "Debe contener letras, y puede contener números y espacios. No se admiten puros números.")
    private String name;

    @NotBlank(message = "El cliente es obligatorio")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[a-zA-ZñÑáéíóúÁÉÍÓÚ])[a-zA-ZñÑáéíóúÁÉÍÓÚ0-9 ]+$", message = "Debe contener letras, y puede contener números y espacios. No se admiten puros números.")
    private String client;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^(?=.*[a-zA-ZñÑáéíóúÁÉÍÓÚ])[a-zA-ZñÑáéíóúÁÉÍÓÚ0-9 ]+$", message = "Debe contener letras, y puede contener números y espacios. No se admiten puros números.")
    private String location;

    private String description;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio puede ser hoy o en el futuro", groups = OnCreate.class)
    private LocalDate startDate;

    @NotBlank(message = "El estado inicial es obligatorio")
    private String status;

    @NotBlank(message = "La prioridad es obligatoria")
    private String priority;

    @NotNull(message = "Debe asignar un responsable al proyecto")
    private Long managerId;
}
