package com.colegio.shuji.tesoreria.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class SobrepagoException extends BusinessException {
  public SobrepagoException(String mensaje) {
    super(mensaje, "SOBREPAGO");
  }
}
