package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record CrearAnioLectivoRequestDto(
    @NotNull @Min(2024) Short anio, @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) {}
