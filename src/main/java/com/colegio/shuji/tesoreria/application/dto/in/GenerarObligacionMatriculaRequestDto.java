package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record GenerarObligacionMatriculaRequestDto(
    @NotNull @Positive Long matriculaId,
    @NotNull LocalDate fechaVencimiento) {}
