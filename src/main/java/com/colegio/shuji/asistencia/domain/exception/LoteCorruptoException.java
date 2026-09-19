package com.colegio.shuji.asistencia.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class LoteCorruptoException extends BusinessException {
  public LoteCorruptoException(String mensaje) {
    super(mensaje, "LOTE_CORRUPTO");
  }
}
