package com.colegio.shuji.evaluacion.application.dto.out;

import java.util.List;

public record TableroRefuerzoResponseDto(
    Short anioLectivoId,
    Short periodoId,
    long totalSesiones,
    long totalInscripciones,
    long pendientesAsistencia,
    long asistieron,
    long faltaron,
    long justificados,
    long estudiantesUnicosEnRefuerzo,
    long evaluacionesRiesgoTotal,
    long evaluacionesSinDerivar,
    List<SesionRefuerzoDetalleResponseDto> sesiones,
    List<EstudianteRiesgoRefuerzoResponseDto> riesgos) {}
