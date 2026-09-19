package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.RefreshTokenRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AuthResponseDto;

/** Caso de uso: Renovación de Access Token a través de un Refresh Token activo y válido. */
public interface RefreshTokenUseCase {

  AuthResponseDto refreshToken(
      RefreshTokenRequestDto refreshTokenRequest, String ipAddress, String userAgent);
}
