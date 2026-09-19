package com.colegio.shuji.tesoreria.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ComprobanteYaEmitidoException extends BusinessException {
  public ComprobanteYaEmitidoException(String mensaje) {
    super(mensaje, "COMPROBANTE_YA_EMITIDO");
  }
}
