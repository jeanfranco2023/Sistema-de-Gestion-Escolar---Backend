package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record CrearPeriodoRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull @Min(1) @Max(4) Short numeroPeriodo,
    @NotBlank @Size(max = 30) String nombre,
    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin) {}
