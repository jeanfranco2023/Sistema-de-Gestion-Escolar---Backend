package com.colegio.shuji.academico.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class AulaOcupadaException extends BusinessException {
  public AulaOcupadaException(String mensaje) {
    super(mensaje, "AULA_OCUPADA");
  }
}
