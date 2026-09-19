package com.colegio.shuji.academico.application.dto.out;

public record VacantesSeccionResponseDto(
    Integer seccionId, int cupoMaximo, int vacantesOcupadas, int disponibles) {}
