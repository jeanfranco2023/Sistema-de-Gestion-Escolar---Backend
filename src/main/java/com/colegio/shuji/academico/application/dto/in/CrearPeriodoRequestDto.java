package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CrearPeriodoRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull @Min(1) @Max(4) Short numeroPeriodo,
    @NotBlank @Size(max = 30) String nombre,
    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin) {}
