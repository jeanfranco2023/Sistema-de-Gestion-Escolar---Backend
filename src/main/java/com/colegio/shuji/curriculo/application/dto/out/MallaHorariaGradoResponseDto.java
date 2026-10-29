package com.colegio.shuji.curriculo.application.dto.out;

public record MallaHorariaGradoResponseDto(
    Long id,
    Short anioLectivoId,
    Short gradoId,
    Short areaCurricularId,
    Short diaSemana,
    Short bloqueHorarioId,
    Boolean activo) {}
