package com.colegio.shuji.curriculo.application.dto.in;

import com.colegio.shuji.curriculo.domain.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

public record ProgramarHorarioRequestDto(
    @NotNull Long asignacionDocenteId,
    @NotNull DiaSemana diaSemana,
    @NotNull Short bloqueHorarioId) {}
