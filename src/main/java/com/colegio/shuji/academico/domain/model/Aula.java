package com.colegio.shuji.academico.domain.model;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Aula {
  private Integer id;
  private String codigo;
  private String nombre;
  private String ubicacion;
  private Short capacidad;
  private Boolean activa;

  public void prepararRegistro() {
    codigo = codigo.trim().toUpperCase();
    nombre = nombre.trim();
    ubicacion = ubicacion == null || ubicacion.isBlank() ? null : ubicacion.trim();
    exigir(capacidad != null && capacidad > 0, "La capacidad del aula debe ser positiva");
    activa = true;
  }
}
