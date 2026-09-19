package com.colegio.shuji.asistencia.domain.model;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
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
public class AsistenciaAula {
  private Long id;
  private Long matriculaId;
  private LocalDate fechaSesion;
  private LocalTime horaRegistro;
  private EstadoAsistenciaAula estado;
  private Long auxiliarUsuarioId;
  private Boolean justificada;
  private String motivoJustificacion;
  private String documentoSustentoUrl;

  public void justificar(String motivo, String documentoUrl) {
    if (estado != EstadoAsistenciaAula.FALTA_INJUSTIFICADA) {
      throw new BusinessException("Solo se justifican inasistencias injustificadas");
    }
    if (motivo == null || motivo.isBlank()) throw new BusinessException("Indique el motivo");
    this.justificada = true;
    this.motivoJustificacion = motivo;
    this.documentoSustentoUrl = documentoUrl;
    this.estado = EstadoAsistenciaAula.FALTA_JUSTIFICADA;
  }

  public void registrarEstado(EstadoAsistenciaAula nuevoEstado) {
    if (nuevoEstado == null || nuevoEstado == EstadoAsistenciaAula.FALTA_JUSTIFICADA)
      throw new BusinessException("Registre la justificación con su motivo");
    this.motivoJustificacion = null;
    this.documentoSustentoUrl = null;
    this.estado = nuevoEstado;
    this.justificada = false;
  }

  public boolean esFalta() {
    return estado == EstadoAsistenciaAula.FALTA_INJUSTIFICADA
        || estado == EstadoAsistenciaAula.FALTA_JUSTIFICADA;
  }

  public boolean esPresente() {
    return estado == EstadoAsistenciaAula.PRESENTE || estado == EstadoAsistenciaAula.TARDANZA;
  }

  public void registrarPor(Long auxiliarId) {
    if (auxiliarId == null) throw new BusinessException("Se requiere responsable de asistencia");
    registrarEstado(estado);
    auxiliarUsuarioId = auxiliarId;
  }
}
