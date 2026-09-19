package com.colegio.shuji.evaluacion.application.dto.out;

import java.util.List;

public record EstudiantesEnRiesgoResponseDto(
    Short periodoId, List<CalificacionResponseDto> calificaciones) {}
