package com.colegio.shuji.evaluacion.application.dto.in;

import com.colegio.shuji.evaluacion.domain.enums.*;
import jakarta.validation.constraints.*;

public record CalificacionItemRequestDto(
    @NotNull Long matriculaId,
    @NotNull Short competenciaId,
    @NotNull CalificacionCualitativa calificacionCualitativa,
    @Size(max = 4000) String conclusionDescriptiva,
    @NotNull Boolean sugerenciaIaUtilizada) {}
