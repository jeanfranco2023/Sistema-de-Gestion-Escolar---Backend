package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.LoginRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AuthResponseDto;

/** Caso de uso: Inicio de sesión de usuario y emisión de credenciales JWT. */
public interface LoginUseCase {

  AuthResponseDto login(LoginRequestDto loginRequest, String ipAddress, String userAgent);
}
