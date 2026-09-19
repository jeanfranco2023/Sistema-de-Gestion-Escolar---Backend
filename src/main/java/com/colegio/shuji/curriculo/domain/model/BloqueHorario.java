package com.colegio.shuji.curriculo.domain.model;

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
public class BloqueHorario {
  private Short id;
  private Short numeroBloque;
  private LocalTime horaInicio;
  private LocalTime horaFin;
  private Boolean esRecreo;

  public void actualizar(Short numeroBloque, LocalTime inicio, LocalTime fin, Boolean esRecreo) {
    this.numeroBloque = numeroBloque;
    this.horaInicio = inicio;
    this.horaFin = fin;
    this.esRecreo = esRecreo;
  }
}
