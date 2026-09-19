package com.colegio.shuji.usuario.application.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de salida con tokens JWT e información del usuario autenticado. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

  private String accessToken;
  private String refreshToken;

  @Builder.Default private String tokenType = "Bearer";

  private Long expiresIn;
  private UserResponseDto user;
}
