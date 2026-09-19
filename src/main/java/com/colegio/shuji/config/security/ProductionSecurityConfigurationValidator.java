package com.colegio.shuji.config.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityConfigurationValidator {

  private final String jwtSecret;

  public ProductionSecurityConfigurationValidator(
      @Value("${app.jwt.secret:}") String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  @PostConstruct
  void validar() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET es obligatorio en entorno de producción");
    }
    if (jwtSecret.contains("shuji_kitamura_proyecto_integrador")
        || "c2h1amlfa2l0YW11cmFfcHJveWVjdG9faW50ZWdyYWRvcl91dHBfMjAyNg==".equals(jwtSecret)) {
      throw new IllegalStateException(
          "JWT_SECRET usa la clave insegura predeterminada de desarrollo. Configure una clave segura de 256 bits vía variable de entorno.");
    }
    if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException(
          "JWT_SECRET debe tener una longitud mínima de 256 bits (32 bytes) para HMAC-SHA256 en producción.");
    }
  }
}
