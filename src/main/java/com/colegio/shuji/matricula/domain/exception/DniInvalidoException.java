package com.colegio.shuji.matricula.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class DniInvalidoException extends BusinessException {
  public DniInvalidoException(String mensaje) {
    super(mensaje, "DNI_INVALIDO");
  }
}
