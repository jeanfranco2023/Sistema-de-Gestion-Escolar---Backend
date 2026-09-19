package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerarObligacionesAnualesRequestDto(
    @NotNull Long matriculaId,
    @NotNull Short conceptoMatriculaId,
    @NotNull Short conceptoPensionId,
    @NotNull LocalDate primerVencimiento) {}
