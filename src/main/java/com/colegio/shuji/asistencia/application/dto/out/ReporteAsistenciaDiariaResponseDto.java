package com.colegio.shuji.asistencia.application.dto.out;

import java.time.LocalDate;
import java.util.List;

public record ReporteAsistenciaDiariaResponseDto(
    LocalDate fecha, List<AsistenciaAulaResponseDto> asistencias) {}
