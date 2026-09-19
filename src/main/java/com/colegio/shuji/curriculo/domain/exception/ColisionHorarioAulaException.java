package com.colegio.shuji.curriculo.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ColisionHorarioAulaException extends BusinessException {
  public ColisionHorarioAulaException(String mensaje) {
    super(mensaje, "COLISION_HORARIO_AULA");
  }
}
