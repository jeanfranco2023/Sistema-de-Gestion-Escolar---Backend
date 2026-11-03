package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoEstudianteRequestDto(
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "ACTIVO|SUSPENDIDO|RETIRADO|TRASLADADO|EGRESADO|INACTIVO",
             message = "El estado debe ser ACTIVO, SUSPENDIDO, RETIRADO, TRASLADADO, EGRESADO o INACTIVO")
    String estado,

    @NotBlank(message = "El motivo del cambio de estado es obligatorio")
    String motivo
) {}
