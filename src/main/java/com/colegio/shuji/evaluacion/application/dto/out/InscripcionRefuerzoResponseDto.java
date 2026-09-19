package com.colegio.shuji.evaluacion.application.dto.out;

import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;

public record InscripcionRefuerzoResponseDto(
    Long id,
    Long sesionRefuerzoId,
    Long estudianteId,
    Long calificacionOrigenId,
    EstadoAsistenciaRefuerzo estadoAsistencia,
    String observaciones) {}
