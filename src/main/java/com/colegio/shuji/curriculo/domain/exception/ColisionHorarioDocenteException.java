package com.colegio.shuji.curriculo.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ColisionHorarioDocenteException extends BusinessException {
  public ColisionHorarioDocenteException(String mensaje) {
    super(mensaje, "COLISION_HORARIO_DOCENTE");
  }
}
