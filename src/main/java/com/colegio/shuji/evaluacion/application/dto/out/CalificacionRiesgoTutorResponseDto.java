package com.colegio.shuji.evaluacion.application.dto.out;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;

public record CalificacionRiesgoTutorResponseDto(
    Long matriculaId, Short periodoId, CalificacionCualitativa nota) {}
