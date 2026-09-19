package com.colegio.shuji.academico.domain.model;

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
public class Grado {
  private Short id;
  private Short nivelId;
  private Short numeroGrado;
  private String nombre;

  public void actualizar(String nuevoNombre, Short nuevoNumeroGrado) {
    this.nombre = nuevoNombre;
    this.numeroGrado = nuevoNumeroGrado;
  }
}
