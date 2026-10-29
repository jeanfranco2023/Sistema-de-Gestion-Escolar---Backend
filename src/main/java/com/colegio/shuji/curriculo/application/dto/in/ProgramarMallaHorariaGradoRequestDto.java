package com.colegio.shuji.curriculo.application.dto.in;

import com.colegio.shuji.curriculo.domain.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

public record ProgramarMallaHorariaGradoRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Short gradoId,
    @NotNull Short areaCurricularId,
    @NotNull DiaSemana diaSemana,
    @NotNull Short bloqueHorarioId) {}
