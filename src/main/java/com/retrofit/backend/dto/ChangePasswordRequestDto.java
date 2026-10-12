package com.retrofit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
        message = "La contraseña debe contener al menos 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial"
    )
    private String newPassword;

    @jakarta.validation.constraints.NotNull(message = "Debe indicar la aceptación de la política de privacidad")
    @jakarta.validation.constraints.AssertTrue(message = "Debe aceptar la Política de Privacidad y Tratamiento de Datos Personales para continuar")
    private Boolean acceptPrivacyPolicy;
}
