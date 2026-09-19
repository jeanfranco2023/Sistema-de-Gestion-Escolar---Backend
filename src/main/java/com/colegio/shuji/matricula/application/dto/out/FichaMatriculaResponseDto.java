package com.colegio.shuji.matricula.application.dto.out;

import java.util.*;

public record FichaMatriculaResponseDto(
    MatriculaResponseDto matricula,
    EstudianteResponseDto estudiante,
    List<ApoderadoResponseDto> apoderados) {}
