package com.colegio.shuji.asistencia.application.dto.out;

import java.time.*;
import java.util.*;

public record ReporteAsistenciaDiariaResponseDto(
    LocalDate fecha, List<AsistenciaAulaResponseDto> asistencias) {}
