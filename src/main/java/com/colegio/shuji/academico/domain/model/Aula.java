package com.colegio.shuji.academico.domain.model;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Aula {
  public static final int CAPACIDAD_MAXIMA = 30;

  private Integer id;
  private String codigo;
  private String nombre;
  private String ubicacion;
  private Short capacidad;
  private Boolean activa;

  public void prepararRegistro() {
    codigo = codigo.trim().toUpperCase(Locale.ROOT);
    nombre = nombre.trim();
    ubicacion = ubicacion == null || ubicacion.isBlank() ? null : ubicacion.trim();
    exigir(capacidad != null && capacidad > 0 && capacidad <= CAPACIDAD_MAXIMA,
        "La capacidad del aula debe estar entre 1 y 30 estudiantes");
    activa = true;
  }

  public void actualizar(String nuevoNombre, String nuevaUbicacion, Short nuevaCapacidad) {
    if (nuevoNombre != null && !nuevoNombre.isBlank()) {
      this.nombre = nuevoNombre.trim();
    }
    if (nuevaUbicacion != null) {
      this.ubicacion = nuevaUbicacion.isBlank() ? null : nuevaUbicacion.trim();
    }
    if (nuevaCapacidad != null) {
      exigir(nuevaCapacidad > 0 && nuevaCapacidad <= CAPACIDAD_MAXIMA,
          "La capacidad del aula debe estar entre 1 y 30 estudiantes");
      this.capacidad = nuevaCapacidad;
    }
  }

  public void cambiarEstado(boolean nuevoEstado) {
    this.activa = nuevoEstado;
  }
}
