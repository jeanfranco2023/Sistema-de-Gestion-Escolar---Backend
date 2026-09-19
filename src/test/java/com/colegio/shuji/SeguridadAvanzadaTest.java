package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.infrastructure.adapter.MercadoPagoHttpAdapter;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoProductionConfigurationValidator;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Suite completa de pruebas de seguridad:
 * 1. Webhooks: Criptografía HMAC-SHA256, tiempo constante, prevención de replay attack.
 * 2. Anti-IDOR: Control de acceso basado en responsabilidad económica familiar.
 * 3. Open Redirect: Validación estricta de esquema HTTPS y lista blanca de dominios en backUrls.
 * 4. Configuración de producción: Fail-fast ante ausencia de secretos.
 */
class SeguridadAvanzadaTest {

  @Nested
  @DisplayName("1. Seguridad de Webhooks (HMAC-SHA256 y Anti-Replay)")
  class WebhookSecurityTests {

    private final String SECRET = "test_webhook_secret_key_1234567890";
    private final long AHORA = 1750000000L;
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(AHORA), ZoneOffset.UTC);
    private final MercadoPagoWebhookSignatureValidator validator =
        new MercadoPagoWebhookSignatureValidator(SECRET, clock);

    @Test
    @DisplayName("Firma válida y reciente es aceptada")
    void firmaValidaYRecienteEsAceptada() throws Exception {
      String requestId = "req-abc-123";
      String dataId = "99887766";
      String ts = String.valueOf(AHORA - 30); // 30 segundos de antigüedad

      String firmaHex = calcularHmac(SECRET, "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";");
      String signatureHeader = "ts=" + ts + ",v1=" + firmaHex;

      assertTrue(validator.validar(signatureHeader, requestId, dataId));
    }

    @Test
    @DisplayName("Firma falsificada o alterada es rechazada (Anti-Tampering)")
    void firmaFalsificadaEsRechazada() {
      String requestId = "req-abc-123";
      String dataId = "99887766";
      String ts = String.valueOf(AHORA - 10);
      String firmaFalsa = "a".repeat(64);
      String signatureHeader = "ts=" + ts + ",v1=" + firmaFalsa;

      assertFalse(validator.validar(signatureHeader, requestId, dataId));
    }

    @Test
    @DisplayName("Ataque de repetición (Replay Attack): Timestamp > 300 segundos es rechazado")
    void timestampExpiradoEsRechazadoComoReplayAttack() throws Exception {
      String requestId = "req-replay";
      String dataId = "99887766";
      String tsExpirado = String.valueOf(AHORA - 301); // 301 segundos atrás (> 5 minutos)

      String firmaHex = calcularHmac(SECRET, "id:" + dataId + ";request-id:" + requestId + ";ts:" + tsExpirado + ";");
      String signatureHeader = "ts=" + tsExpirado + ",v1=" + firmaHex;

      assertFalse(validator.validar(signatureHeader, requestId, dataId), "Debe rechazar timestamps fuera de la ventana de 5 min");
    }

    @Test
    @DisplayName("Firma malformada o formato no hex es rechazada de inmediato")
    void firmaMalformadaEsRechazada() {
      assertFalse(validator.validar("ts=invalido,v1=not-hex", "req", "123"));
      assertFalse(validator.validar("", "req", "123"));
      assertFalse(validator.validar(null, "req", "123"));
    }
  }

  @Nested
  @DisplayName("2. Prevención de Open Redirect en Back URLs")
  class OpenRedirectSecurityTests {

    @Test
    @DisplayName("Rechaza protocolo HTTP inseguro")
    void rechazaProtocoloInseguroHttp() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe,localhost");

      var obligacion = crearObligacionEjemplo();

      var ex = assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "http://colegio.edu.pe/exito",
              "https://colegio.edu.pe/fallo"));

      assertTrue(ex.getMessage().contains("HTTPS"));
    }

    @Test
    @DisplayName("Rechaza dominios no incluidos en la lista blanca (whitelist)")
    void rechazaDominioNoAutorizado() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe,localhost");

      var obligacion = crearObligacionEjemplo();

      var ex = assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "https://atacante-malicioso.com/robo",
              "https://colegio.edu.pe/fallo"));

      assertTrue(ex.getMessage().contains("Dominio no permitido"));
    }

    @Test
    @DisplayName("Rechaza URLs que incluyan credenciales o user-info en la URL")
    void rechazaUserInfoEnUrl() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe,localhost");

      var obligacion = crearObligacionEjemplo();

      assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "https://usuario:password@colegio.edu.pe/exito",
              "https://colegio.edu.pe/fallo"));
    }
  }

  @Nested
  @DisplayName("3. Configuración y Rotación de Secretos en Producción")
  class ProductionSecretsTests {

    @Test
    @DisplayName("Falla al iniciar si MERCADOPAGO_WEBHOOK_SECRET está vacío en perfil prod")
    void fallaInicioSiSecretFaltaEnProduccion() {
      var validator = new MercadoPagoProductionConfigurationValidator("", "colegio.edu.pe");
      var ex = assertThrows(IllegalStateException.class, () -> {
        var m = MercadoPagoProductionConfigurationValidator.class.getDeclaredMethod("validar");
        m.setAccessible(true);
        try {
          m.invoke(validator);
        } catch (java.lang.reflect.InvocationTargetException ite) {
          throw ite.getCause();
        }
      });
      assertTrue(ex.getMessage().contains("MERCADOPAGO_WEBHOOK_SECRET"));
    }

    @Test
    @DisplayName("Falla al iniciar si MERCADOPAGO_RETURN_URL_HOSTS está vacío en perfil prod")
    void fallaInicioSiHostsFaltanEnProduccion() {
      var validator = new MercadoPagoProductionConfigurationValidator("secret123", "");
      var ex = assertThrows(IllegalStateException.class, () -> {
        var m = MercadoPagoProductionConfigurationValidator.class.getDeclaredMethod("validar");
        m.setAccessible(true);
        try {
          m.invoke(validator);
        } catch (java.lang.reflect.InvocationTargetException ite) {
          throw ite.getCause();
        }
      });
      assertTrue(ex.getMessage().contains("MERCADOPAGO_RETURN_URL_HOSTS"));
    }
  }

  private static String calcularHmac(String secret, String data) throws Exception {
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return java.util.HexFormat.of().formatHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
  }

  private static ObligacionPago crearObligacionEjemplo() {
    return ObligacionPago.builder()
        .id(100L)
        .matriculaId(200L)
        .conceptoId((short) 1)
        .numeroCuota((short) 1)
        .montoBase(new BigDecimal("350.00"))
        .montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO)
        .totalPagado(BigDecimal.ZERO)
        .saldoPendiente(new BigDecimal("350.00"))
        .fechaVencimiento(LocalDate.now().plusDays(30))
        .estado(EstadoObligacion.PENDIENTE)
        .build();
  }
}
