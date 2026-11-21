package com.colegio.shuji.asistencia.application.port.out;

import java.util.Locale;

public record SeccionAsistencia(
    Long matriculaId, Integer seccionId, String grado, String nivel, String letra) {

  public String etiqueta() {
    if (seccionId == null) return "Sin sección";
    String nombre = grado == null || grado.isBlank() ? "Grado sin datos" : grado.trim();
    if (nivel != null && !nivel.isBlank()
        && !nombre.toLowerCase(Locale.ROOT).contains(nivel.trim().toLowerCase(Locale.ROOT))) {
      nombre += " · " + nivel.trim();
    }
    return nombre + " · Sección " + (letra == null ? "sin datos" : letra);
  }
}
