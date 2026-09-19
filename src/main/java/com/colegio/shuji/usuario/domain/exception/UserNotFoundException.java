package com.colegio.shuji.usuario.domain.exception;

import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;

/** Excepción lanzada cuando un usuario no es encontrado en el sistema. */
public class UserNotFoundException extends ResourceNotFoundException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(Long id) {
    super("Usuario", "id", id);
  }

  public UserNotFoundException(String field, Object value) {
    super("Usuario", field, value);
  }
}
