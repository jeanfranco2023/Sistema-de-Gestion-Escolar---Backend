package com.colegio.shuji.convivencia.application.dto.out;

import java.util.*;

public record HistorialConductualEstudianteResponseDto(
    Long matriculaId, List<IncidenciaResponseDto> incidencias) {}
