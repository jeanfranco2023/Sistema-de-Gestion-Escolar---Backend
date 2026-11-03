package com.colegio.shuji.evaluacion.application.dto.out;

import java.util.List;

public record CuadroHonorResponseDto(
    Short anioLectivoId,
    Short gradoId,
    Integer seccionId,
    String nivelNombre,
    String gradoNombre,
    String seccionLetra,
    String disclaimer,
    List<RankingEstudianteResponseDto> estudiantes
) {}
