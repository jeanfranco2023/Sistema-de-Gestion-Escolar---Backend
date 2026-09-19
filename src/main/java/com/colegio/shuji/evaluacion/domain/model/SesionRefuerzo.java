package com.colegio.shuji.evaluacion.domain.model;

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
public class SesionRefuerzo {
  private Long id;
  private Short anioLectivoId;
  private Short periodoAcademicoId;
  private Short areaCurricularId;
  private Long docenteUsuarioId;
  private String tema;
  private LocalDate fechaProgramada;
  private LocalTime horaInicio;
  private LocalTime horaFin;
  private String aulaAsignada;

  public void reprogramar(
      LocalDate nuevaFecha, LocalTime nuevoInicio, LocalTime nuevoFin, String nuevaAula) {
    this.fechaProgramada = nuevaFecha;
    this.horaInicio = nuevoInicio;
    this.horaFin = nuevoFin;
    this.aulaAsignada = nuevaAula;
  }

  public void actualizarTema(String nuevoTema) {
    this.tema = nuevoTema;
  }
}
