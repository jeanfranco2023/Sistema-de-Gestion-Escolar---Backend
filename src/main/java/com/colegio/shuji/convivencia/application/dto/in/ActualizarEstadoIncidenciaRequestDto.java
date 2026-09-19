package com.colegio.shuji.convivencia.application.dto.in;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoIncidenciaRequestDto(@NotNull EstadoIncidencia estado) {}
