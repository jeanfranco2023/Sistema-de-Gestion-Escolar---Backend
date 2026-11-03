package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoApoderadoRequestDto(
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "ACTIVO|INACTIVO|SUSPENDIDO",
             message = "El estado debe ser ACTIVO, INACTIVO o SUSPENDIDO")
    String estado,

    @NotBlank(message = "El motivo del cambio de estado es obligatorio")
    String motivo
) {}
