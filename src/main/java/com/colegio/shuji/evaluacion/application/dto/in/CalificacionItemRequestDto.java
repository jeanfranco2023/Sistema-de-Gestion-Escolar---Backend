package com.colegio.shuji.evaluacion.application.dto.in;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalificacionItemRequestDto(
    @NotNull Long matriculaId,
    @NotNull Short competenciaId,
    @NotNull CalificacionCualitativa calificacionCualitativa,
    @Size(max = 4000) String conclusionDescriptiva,
    @NotNull Boolean sugerenciaIaUtilizada) {}
