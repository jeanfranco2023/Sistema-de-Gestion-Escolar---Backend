package com.colegio.shuji.usuario.application.port.out;

public interface PasswordHashPort {
  String encode(CharSequence password);

  boolean matches(CharSequence password, String encoded);
}
