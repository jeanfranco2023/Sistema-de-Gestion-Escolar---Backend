package com.colegio.shuji.academico.application.dto.out;

import java.time.LocalDate;

public record AnioLectivoResponseDto(
    Short id, Short anio, LocalDate fechaInicio, LocalDate fechaFin, Boolean abierto) {}
