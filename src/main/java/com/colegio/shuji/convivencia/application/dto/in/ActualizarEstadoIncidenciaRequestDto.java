package com.colegio.shuji.convivencia.application.dto.in;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEstadoIncidenciaRequestDto(
    @NotNull EstadoIncidencia estado,
    @NotBlank @Size(min = 12, max = 1000) String observacionGestion,
    @Size(max = 300) String resultadoVerificado,
    @Size(max = 120) String participantes,
    @Size(max = 130) String comunicacionApoderado) {
  public ActualizarEstadoIncidenciaRequestDto(EstadoIncidencia estado, String observacionGestion) {
    this(estado, observacionGestion, null, null, null);
  }
}
