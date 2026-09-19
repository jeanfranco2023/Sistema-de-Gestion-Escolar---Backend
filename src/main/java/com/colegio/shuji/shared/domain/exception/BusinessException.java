package com.colegio.shuji.shared.domain.exception;

import lombok.Getter;

/** Excepción base para todas las reglas y errores de negocio del sistema. */
@Getter
public class BusinessException extends RuntimeException {

  private final String errorCode;

  public BusinessException(String message) {
    super(message);
    this.errorCode = "BUSINESS_ERROR";
  }

  public BusinessException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "BUSINESS_ERROR";
  }

  public BusinessException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
