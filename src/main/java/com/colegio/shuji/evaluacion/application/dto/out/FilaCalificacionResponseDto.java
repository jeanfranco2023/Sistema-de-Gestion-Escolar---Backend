package com.colegio.shuji.evaluacion.application.dto.out;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;

public record FilaCalificacionResponseDto(
    Long matriculaId,
    Short competenciaId,
    CalificacionCualitativa calificacionCualitativa,
    String conclusionDescriptiva,
    Boolean requiereRefuerzo) {}
