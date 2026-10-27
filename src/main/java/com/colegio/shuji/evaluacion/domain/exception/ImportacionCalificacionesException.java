package com.colegio.shuji.evaluacion.domain.exception;

import java.util.Map;

public class ImportacionCalificacionesException extends RuntimeException {
  private final Map<String, String> errores;
  public ImportacionCalificacionesException(Map<String, String> errores) {
    super("Corrige las filas observadas. No se guardó ninguna calificación.");
    this.errores = Map.copyOf(errores);
  }
  public Map<String, String> errores() { return errores; }
}
