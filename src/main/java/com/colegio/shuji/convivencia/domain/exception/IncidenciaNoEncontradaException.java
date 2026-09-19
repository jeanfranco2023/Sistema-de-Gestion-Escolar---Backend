package com.colegio.shuji.convivencia.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class IncidenciaNoEncontradaException extends BusinessException {
  public IncidenciaNoEncontradaException(String mensaje) {
    super(mensaje, "INCIDENCIA_NO_ENCONTRADA");
  }
}
