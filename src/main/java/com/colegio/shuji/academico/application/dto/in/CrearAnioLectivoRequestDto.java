package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CrearAnioLectivoRequestDto(
    @NotNull @Min(2024) Short anio, @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) {}
