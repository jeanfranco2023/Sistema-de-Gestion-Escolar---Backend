package com.colegio.shuji.usuario.application.port.out;

import java.util.List;

public interface TokenPort {
  String generateToken(Long userId, String username, List<String> roles);

  String generateRefreshToken(Long userId, String username);

  boolean validateToken(String token);

  long getJwtExpirationMs();

  long getJwtRefreshExpirationMs();
}
