package com.colegio.shuji.evaluacion.application.dto.out;

import java.util.*;

public record LibretaNotasResponseDto(
    Long matriculaId, List<CalificacionResponseDto> calificaciones) {}
