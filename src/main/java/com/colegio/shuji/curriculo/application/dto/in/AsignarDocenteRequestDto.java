package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.NotNull;

public record AsignarDocenteRequestDto(
    @NotNull Long docenteUsuarioId,
    @NotNull Integer seccionId,
    @NotNull Short anioLectivoId,
    @NotNull Short nivelId,
    @NotNull Short areaCurricularId) {}
