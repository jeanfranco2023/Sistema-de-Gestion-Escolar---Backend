package com.colegio.shuji.academico.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class CupoAgotadoException extends BusinessException {
  public CupoAgotadoException(String mensaje) {
    super(mensaje, "CUPO_AGOTADO");
  }
}
