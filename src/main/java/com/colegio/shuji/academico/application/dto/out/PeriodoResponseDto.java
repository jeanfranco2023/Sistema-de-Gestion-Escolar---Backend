package com.colegio.shuji.academico.application.dto.out;

import java.time.LocalDate;

public record PeriodoResponseDto(
    Short id,
    Short anioLectivoId,
    Short numeroPeriodo,
    String nombre,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Boolean cerrado) {}
