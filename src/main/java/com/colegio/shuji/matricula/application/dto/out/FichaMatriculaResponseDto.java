package com.colegio.shuji.matricula.application.dto.out;

import java.util.List;

public record FichaMatriculaResponseDto(
    MatriculaResponseDto matricula,
    EstudianteResponseDto estudiante,
    List<ApoderadoResponseDto> apoderados) {}
