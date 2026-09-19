package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record SolicitarMatriculaRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Long estudianteId,
    @NotNull Integer seccionId,
    @Future OffsetDateTime reservaExpiraAt,
    @Size(max = 4000) String observaciones) {}
