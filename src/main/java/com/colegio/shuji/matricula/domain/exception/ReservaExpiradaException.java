package com.colegio.shuji.matricula.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class ReservaExpiradaException extends BusinessException {
  public ReservaExpiradaException(String mensaje) {
    super(mensaje, "RESERVA_EXPIRADA");
  }
}
