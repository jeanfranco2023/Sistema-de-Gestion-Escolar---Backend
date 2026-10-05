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

  public void actualizar(String nuevoNombre) {
    this.nombre = nuevoNombre;
  }
}
