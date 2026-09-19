package com.colegio.shuji.academico.domain.model;

import com.colegio.shuji.shared.domain.model.Reglas;
import java.time.LocalDate;
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
public class PeriodoAcademico {
  private Short id;
  private Short anioLectivoId;
  private Short numeroPeriodo;
  private String nombre;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private Boolean cerrado;

  public void verificarAbierto() {
    if (Boolean.TRUE.equals(cerrado)) {
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "El período académico está cerrado");
    }
  }

  public void abrir() {
    Reglas.fechas(fechaInicio, fechaFin);
    this.cerrado = false;
  }

  public void cerrar() {
    this.cerrado = true;
  }

  public boolean estaCerrado() {
    return Boolean.TRUE.equals(this.cerrado);
  }

  public void actualizar(String nuevoNombre, LocalDate nuevoInicio, LocalDate nuevoFin) {
    Reglas.fechas(nuevoInicio, nuevoFin);
    this.nombre = nuevoNombre;
    this.fechaInicio = nuevoInicio;
    this.fechaFin = nuevoFin;
  }
}
