package com.colegio.shuji.usuario.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ReglaNegocioException extends BusinessException {

  public ReglaNegocioException(String message) {
    super(message, "REGLA_NEGOCIO_ERROR");
  }

  public ReglaNegocioException(String message, String errorCode) {
    super(message, errorCode);
  }
}
