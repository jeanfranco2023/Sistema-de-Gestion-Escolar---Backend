package com.colegio.shuji.curriculo.domain.model;

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
public class Competencia {
  private Short id;
  private Short areaId;
  private Short numeroOrden;
  private String nombre;
  private String descripcion;

  public void actualizar(Short nuevoOrden, String nuevoNombre, String nuevaDescripcion) {
    this.numeroOrden = nuevoOrden;
    this.nombre = nuevoNombre;
    this.descripcion = nuevaDescripcion;
  }
}
