package com.colegio.shuji.academico.application.dto.out;

public record SeccionResponseDto(
    Integer id,
    Short anioLectivoId,
    Short gradoId,
    Short nivelId,
    String letra,
    Short cupoMaximo,
    Short vacantesOcupadas,
    String aulaFisica) {}
