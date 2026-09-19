package com.colegio.shuji.curriculo.application.dto.out;

public record HorarioSeccionResponseDto(
    Long id,
    Short anioLectivoId,
    Integer seccionId,
    Long docenteUsuarioId,
    Long asignacionDocenteId,
    Short diaSemana,
    Short bloqueHorarioId) {}
