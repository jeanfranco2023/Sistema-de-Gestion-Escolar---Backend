package com.colegio.shuji.tesoreria.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoWebhookSignatureValidator {
  private static final long MAX_DESFASE_SEGUNDOS = 300;

  private final List<String> secrets = new ArrayList<>();
  private final Clock clock;

  public MercadoPagoWebhookSignatureValidator(
      @Value("${integraciones.pagos.mercadopago.webhook-secret:}") String secret,
      @Value("${integraciones.pagos.mercadopago.webhook-previous-secret:}") String previousSecret,
      java.util.Optional<Clock> clockOptional) {
    if (secret != null && !secret.isBlank()) {
      this.secrets.add(secret.trim());
    }
    if (previousSecret != null && !previousSecret.isBlank()) {
      this.secrets.add(previousSecret.trim());
    }
    this.clock = clockOptional.orElseGet(Clock::systemUTC);
  }



  public boolean estaConfigurado() {
    return !secrets.isEmpty();
  }

  public boolean validar(String signature, String requestId, String dataId) {
    if (!estaConfigurado() || signature == null || signature.isBlank() || dataId == null) return false;
    String ts = null;
    String v1 = null;
    for (String part : signature.split(",")) {
      String[] kv = part.trim().split("=", 2);
      if (kv.length != 2) continue;
      if ("ts".equalsIgnoreCase(kv[0].trim())) ts = kv[1].trim();
      if ("v1".equalsIgnoreCase(kv[0].trim())) v1 = kv[1].trim();
    }
    if (ts == null || v1 == null || !v1.matches("(?i)[0-9a-f]{64}")) return false;

    final long timestamp;
    try {
      timestamp = Long.parseLong(ts);
    } catch (NumberFormatException ex) {
      return false;
    }
    long ahora = clock.instant().getEpochSecond();
    if (timestamp < ahora - MAX_DESFASE_SEGUNDOS || timestamp > ahora + MAX_DESFASE_SEGUNDOS) {
      return false;
    }

    try {
      StringBuilder manifest = new StringBuilder("id:").append(dataId).append(';');
      if (requestId != null && !requestId.isBlank()) {
        manifest.append("request-id:").append(requestId).append(';');
      }
      manifest.append("ts:").append(ts).append(';');
      byte[] manifestBytes = manifest.toString().getBytes(StandardCharsets.UTF_8);
      byte[] recibida = java.util.HexFormat.of().parseHex(v1);

      for (String s : secrets) {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(s.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] calculada = hmac.doFinal(manifestBytes);
        if (MessageDigest.isEqual(calculada, recibida)) {
          return true;
        }
      }
      return false;
    } catch (Exception ex) {
      return false;
    }
  }
}
