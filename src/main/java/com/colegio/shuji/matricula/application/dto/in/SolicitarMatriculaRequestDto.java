package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record SolicitarMatriculaRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Long estudianteId,
    @NotNull Integer seccionId,
    @Future OffsetDateTime reservaExpiraAt,
    @Size(max = 4000) String observaciones) {}
