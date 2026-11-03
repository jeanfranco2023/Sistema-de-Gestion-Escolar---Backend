package com.colegio.shuji.usuario.application.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para establecer la contraseña definitiva en el primer inicio de sesión")
public record CambiarPasswordInicialRequestDto(
    @Schema(description = "Contraseña temporal o actual", example = "Temp1234#")
    @NotBlank(message = "La contraseña actual es obligatoria")
    String passwordActual,

    @Schema(description = "Nueva contraseña segura", example = "MiNuevaPassword2026!")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "La nueva contraseña debe contener al menos una letra mayúscula, una letra minúscula y un número"
    )
    String nuevoPassword
) {}
