package com.colegio.shuji.evaluacion.domain.model;

import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionRefuerzo {
  private Long id;
  private Long sesionRefuerzoId;
  private Long matriculaId;
  private Long estudianteId;
  private Long calificacionOrigenId;
  private EstadoAsistenciaRefuerzo estadoAsistencia;
  private String observaciones;

  public void registrarAsistencia(EstadoAsistenciaRefuerzo estado, String obs) {
    if (estado == null || estado == EstadoAsistenciaRefuerzo.PENDIENTE)
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "Indique la asistencia");
    if (estado == EstadoAsistenciaRefuerzo.JUSTIFICADO && (obs == null || obs.isBlank()))
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "Indique la justificación");
    this.estadoAsistencia = estado;
    this.observaciones = obs;
  }

  public void justificar(String motivo) {
    registrarAsistencia(EstadoAsistenciaRefuerzo.JUSTIFICADO, motivo);
  }
}
