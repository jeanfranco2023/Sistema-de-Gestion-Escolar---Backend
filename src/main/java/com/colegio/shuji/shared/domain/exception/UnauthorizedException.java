package com.colegio.shuji.shared.domain.exception;

import lombok.Getter;

/** Excepción lanzada cuando la autenticación o autorización es inválida o inexistente. */
@Getter
public class UnauthorizedException extends BusinessException {

  public UnauthorizedException(String message) {
    super(message, "UNAUTHORIZED");
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, "UNAUTHORIZED", cause);
  }
}
