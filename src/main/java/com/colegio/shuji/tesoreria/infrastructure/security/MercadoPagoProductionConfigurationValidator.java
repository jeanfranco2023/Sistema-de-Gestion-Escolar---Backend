package com.colegio.shuji.tesoreria.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class MercadoPagoProductionConfigurationValidator {
  private final String webhookSecret;
  private final String returnUrlHosts;
  private final String accessToken;

  public MercadoPagoProductionConfigurationValidator(
      @Value("${integraciones.pagos.mercadopago.webhook-secret:}") String webhookSecret,
      @Value("${integraciones.pagos.mercadopago.return-url-hosts:}") String returnUrlHosts,
      @Value("${integraciones.pagos.mercadopago.access-token:}") String accessToken) {
    this.webhookSecret = webhookSecret;
    this.returnUrlHosts = returnUrlHosts;
    this.accessToken = accessToken;
  }

  @PostConstruct
  void validar() {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new IllegalStateException("MERCADOPAGO_WEBHOOK_SECRET es obligatorio en producción");
    }
    if (returnUrlHosts == null || returnUrlHosts.isBlank()) {
      throw new IllegalStateException("MERCADOPAGO_RETURN_URL_HOSTS es obligatorio en producción");
    }
    if (accessToken == null || accessToken.isBlank() || accessToken.startsWith("TEST-") || accessToken.length() < 20) {
      throw new IllegalStateException("MERCADOPAGO_ACCESS_TOKEN es obligatorio y debe ser una credencial productiva válida (APP_USR-) en producción");
    }
  }
}
