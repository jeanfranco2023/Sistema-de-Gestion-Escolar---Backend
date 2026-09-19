package com.colegio.shuji.usuario.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

/** Excepción lanzada cuando se intenta registrar un usuario con username o email ya en uso. */
public class UserAlreadyExistsException extends BusinessException {

  public UserAlreadyExistsException(String message) {
    super(message, "USER_ALREADY_EXISTS");
  }

  public UserAlreadyExistsException(String field, String value) {
    super(
        String.format(
            "El usuario con %s '%s' ya se encuentra registrado en el sistema.", field, value),
        "USER_ALREADY_EXISTS");
  }
}
