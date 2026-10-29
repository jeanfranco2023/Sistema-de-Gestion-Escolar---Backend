package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import java.time.LocalDate;
import java.time.LocalTime;

public record AsistenciaAulaResponseDto(
    Long id,
    Long matriculaId,
    String nombreEstudiante,
    LocalDate fechaSesion,
    LocalTime horaRegistro,
    EstadoAsistenciaAula estado,
    Long auxiliarUsuarioId,
    Boolean justificada,
    String motivoJustificacion,
    boolean tieneDocumentoSustento) {}
