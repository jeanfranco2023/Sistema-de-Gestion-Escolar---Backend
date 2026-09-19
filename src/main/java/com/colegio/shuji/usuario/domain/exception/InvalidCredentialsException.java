package com.colegio.shuji.usuario.domain.exception;

import com.colegio.shuji.shared.domain.exception.UnauthorizedException;

/** Excepción lanzada cuando las credenciales de acceso (usuario/email/contraseña) no coinciden. */
public class InvalidCredentialsException extends UnauthorizedException {

  public InvalidCredentialsException() {
    super("Nombre de usuario o contraseña incorrectos.");
  }

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
