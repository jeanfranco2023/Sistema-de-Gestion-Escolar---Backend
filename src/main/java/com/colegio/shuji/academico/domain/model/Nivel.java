package com.colegio.shuji.academico.domain.model;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;
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
public class Nivel {
  private Short id;
  private NivelCodigo codigo;
  private String nombre;
  private String descripcion;

  public void actualizar(String nuevoNombre, String nuevaDescripcion) {
    this.nombre = nuevoNombre;
    this.descripcion = nuevaDescripcion;
  }
}
