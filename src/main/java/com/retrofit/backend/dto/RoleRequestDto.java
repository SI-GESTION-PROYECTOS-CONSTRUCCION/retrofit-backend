package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class RoleRequestDto {
    @NotBlank(message = "El nombre del rol es obligatorio")
    private String name;
    
    @NotBlank(message = "La descripción del rol es obligatoria")
    private String description;
    
    @NotEmpty(message = "El rol debe tener al menos un permiso asignado")
    private Set<Long> permissionIds;
}