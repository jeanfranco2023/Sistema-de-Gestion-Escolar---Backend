package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import java.time.LocalDate;

public record ConciliacionAsistenciaResponseDto(
    Long id,
    LocalDate fecha,
    Long estudianteId,
    Boolean marcoPorteria,
    Boolean presenteAula,
    TipoDiscrepancia tipoDiscrepancia,
    Boolean alertaNotificada) {}
