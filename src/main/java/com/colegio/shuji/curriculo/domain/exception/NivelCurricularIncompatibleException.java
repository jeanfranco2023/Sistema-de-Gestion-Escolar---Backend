package com.colegio.shuji.curriculo.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class NivelCurricularIncompatibleException extends BusinessException {
  public NivelCurricularIncompatibleException(String mensaje) {
    super(mensaje, "NIVEL_CURRICULAR_INCOMPATIBLE");
  }
}
