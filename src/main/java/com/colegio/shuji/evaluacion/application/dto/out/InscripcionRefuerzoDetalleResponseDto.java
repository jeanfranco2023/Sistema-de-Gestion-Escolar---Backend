package com.colegio.shuji.evaluacion.application.dto.out;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
public record InscripcionRefuerzoDetalleResponseDto(Long id,Long sesionRefuerzoId,Long estudianteId,
    Long calificacionOrigenId,EstadoAsistenciaRefuerzo estadoAsistencia,String observaciones,String nombreEstudiante) {}
