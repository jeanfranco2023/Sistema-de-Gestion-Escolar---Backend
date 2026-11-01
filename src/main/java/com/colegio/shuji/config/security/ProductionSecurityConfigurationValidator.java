package com.colegio.shuji.config.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    if (contieneSecretoPredeterminado()) {
      throw new IllegalStateException(
          "JWT_SECRET usa la clave insegura predeterminada de desarrollo. Configure una clave segura de 256 bits vía variable de entorno.");
    }
    if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException(
          "JWT_SECRET debe tener una longitud mínima de 256 bits (32 bytes) para HMAC-SHA256 en producción.");
    }
  }

  private boolean contieneSecretoPredeterminado() {
    String marcador = "shuji_kitamura_proyecto_integrador";
    if (jwtSecret.contains(marcador)) return true;
    try {
      return new String(Base64.getDecoder().decode(jwtSecret), StandardCharsets.UTF_8).contains(marcador);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
