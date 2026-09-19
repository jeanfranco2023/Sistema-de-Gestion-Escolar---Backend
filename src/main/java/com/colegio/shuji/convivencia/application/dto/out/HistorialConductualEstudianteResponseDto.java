package com.colegio.shuji.convivencia.application.dto.out;

import java.util.List;

public record HistorialConductualEstudianteResponseDto(
    Long matriculaId, List<IncidenciaResponseDto> incidencias) {}
