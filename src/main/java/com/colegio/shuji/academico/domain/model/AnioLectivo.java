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
public class AnioLectivo {
  private Short id;
  private Short anio;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private Boolean abierto;

  public void verificarAbierto() {
    if (!Boolean.TRUE.equals(abierto)) {
      throw new com.colegio.shuji.academico.domain.exception.AnioLectivoCerradoException(
          "El año lectivo está cerrado");
    }
  }

  public void abrir() {
    Reglas.fechas(fechaInicio, fechaFin);
    this.abierto = true;
  }

  public void cerrar() {
    this.abierto = false;
  }

  public boolean estaAbierto() {
    return Boolean.TRUE.equals(this.abierto);
  }

  public void actualizarFechas(LocalDate inicio, LocalDate fin) {
    Reglas.fechas(inicio, fin);
    this.fechaInicio = inicio;
    this.fechaFin = fin;
  }
}
