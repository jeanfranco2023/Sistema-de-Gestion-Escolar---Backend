package com.colegio.shuji.comunicado.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ComunicadoNoEncontradoException extends BusinessException {
  public ComunicadoNoEncontradoException(String mensaje) {
    super(mensaje, "COMUNICADO_NO_ENCONTRADO");
  }
}
