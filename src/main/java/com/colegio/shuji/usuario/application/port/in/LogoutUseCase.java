package com.colegio.shuji.usuario.application.port.in;

/**
 * Caso de uso para revocar la sesión activa del usuario al cerrar sesión.
 */
public interface LogoutUseCase {

  /**
   * Invalida y elimina la sesión asociada al refresh token dado.
   *
   * @param refreshToken token de refresco a invalidar
   */
  void logout(String refreshToken);
}
