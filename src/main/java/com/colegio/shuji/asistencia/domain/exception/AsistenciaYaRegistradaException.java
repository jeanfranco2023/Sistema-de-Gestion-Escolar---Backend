package com.colegio.shuji.asistencia.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class AsistenciaYaRegistradaException extends BusinessException {
  public AsistenciaYaRegistradaException(String mensaje) {
    super(mensaje, "ASISTENCIA_YA_REGISTRADA");
  }
}
