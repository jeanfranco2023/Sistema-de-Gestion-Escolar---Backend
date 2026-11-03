package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ReenviarTokenSolicitudRequestDto(
    @NotBlank(message = "El DNI del estudiante es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI del estudiante debe tener 8 dígitos numéricos")
    String numeroDocumentoEstudiante,

    @NotNull(message = "El año lectivo es obligatorio")
    Short anioLectivoId
) {}
