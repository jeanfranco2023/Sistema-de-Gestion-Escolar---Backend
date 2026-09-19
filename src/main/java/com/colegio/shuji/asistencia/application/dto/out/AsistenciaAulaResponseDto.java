package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.*;
import java.time.*;

public record AsistenciaAulaResponseDto(
    Long id,
    Long matriculaId,
    LocalDate fechaSesion,
    LocalTime horaRegistro,
    EstadoAsistenciaAula estado,
    Long auxiliarUsuarioId,
    Boolean justificada,
    String motivoJustificacion,
    String documentoSustentoUrl) {}
