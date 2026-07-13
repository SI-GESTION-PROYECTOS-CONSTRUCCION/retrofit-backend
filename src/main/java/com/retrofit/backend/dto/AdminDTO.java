package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AdminDTO {
    private String email;
    private String username;
    private String password;
    private String role;
    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ ]+$", message = "Solo se permiten letras")
    private String name;
    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ ]+$", message = "Solo se permiten letras")
    private String lastName;
}
