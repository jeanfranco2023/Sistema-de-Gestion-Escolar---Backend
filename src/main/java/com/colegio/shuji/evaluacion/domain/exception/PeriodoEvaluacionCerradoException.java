package com.colegio.shuji.evaluacion.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class PeriodoEvaluacionCerradoException extends BusinessException {
  public PeriodoEvaluacionCerradoException(String mensaje) {
    super(mensaje, "PERIODO_EVALUACION_CERRADO");
  }
}
