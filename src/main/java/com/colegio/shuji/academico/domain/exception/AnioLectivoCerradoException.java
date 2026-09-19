package com.colegio.shuji.academico.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class AnioLectivoCerradoException extends BusinessException {
  public AnioLectivoCerradoException(String mensaje) {
    super(mensaje, "ANIO_LECTIVO_CERRADO");
  }
}
