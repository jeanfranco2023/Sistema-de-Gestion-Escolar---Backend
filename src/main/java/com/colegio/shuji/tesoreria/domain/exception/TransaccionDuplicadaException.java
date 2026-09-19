package com.colegio.shuji.tesoreria.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class TransaccionDuplicadaException extends BusinessException {
  public TransaccionDuplicadaException(String mensaje) {
    super(mensaje, "TRANSACCION_DUPLICADA");
  }
}
