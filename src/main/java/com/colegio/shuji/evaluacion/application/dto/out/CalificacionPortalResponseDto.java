package com.colegio.shuji.evaluacion.application.dto.out;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
public record CalificacionPortalResponseDto(Short areaId, String areaNombre, Short competenciaId,
    String competenciaNombre, Short periodoId, String periodoNombre, CalificacionCualitativa nota, String conclusion) {}
