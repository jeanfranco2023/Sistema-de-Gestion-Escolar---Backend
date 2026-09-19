package com.colegio.shuji.evaluacion.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class CalificacionInvalidaException extends BusinessException {
  public CalificacionInvalidaException(String mensaje) {
    super(mensaje, "CALIFICACION_INVALIDA");
  }
}
