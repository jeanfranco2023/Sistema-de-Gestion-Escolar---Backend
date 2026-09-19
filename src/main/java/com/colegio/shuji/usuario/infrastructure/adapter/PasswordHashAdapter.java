package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordHashAdapter implements PasswordHashPort {
  private final PasswordEncoder encoder;

  @Override
  public String encode(CharSequence password) {
    return encoder.encode(password);
  }

  @Override
  public boolean matches(CharSequence password, String encoded) {
    return encoder.matches(password, encoded);
  }
}
