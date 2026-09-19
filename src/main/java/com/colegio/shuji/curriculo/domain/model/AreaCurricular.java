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
public class AreaCurricular {
  private Short id;
  private Short nivelId;
  private String codigo;
  private String nombre;

  public void actualizar(String nuevoCodigo, String nuevoNombre) {
    this.codigo = nuevoCodigo;
    this.nombre = nuevoNombre;
  }
}
