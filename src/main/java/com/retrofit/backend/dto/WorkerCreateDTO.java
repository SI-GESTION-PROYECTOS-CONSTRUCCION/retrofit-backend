package com.retrofit.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerCreateDTO {
    // Datos del trabajador
    private String position;
    private String dni;
    private String phone;

    // Para crear un User si es necesario
    private String name;
    private String username;
    private String lastName;
    private String email;

    // Datos de la cuenta
    private Boolean createAccount;
    private String password;
    private String role;
}