package com.colegio.shuji.curriculo.application.dto.out;

public record AsignacionDocenteResponseDto(
    Long id,
    Long docenteUsuarioId,
    Integer seccionId,
    Short anioLectivoId,
    Short nivelId,
    Short areaCurricularId) {}
