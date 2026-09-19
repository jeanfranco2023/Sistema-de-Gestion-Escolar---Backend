package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Contrato con un verificador privado que consulta las APIs de Culqi/Niubiz. No acepta importes ni
 * estados provenientes del webhook público.
 */
@Component
public class VerificarPagoHttpAdapter implements VerificarPagoPort {
  private final String url, webhookSecret;
  private final RestClient client;

  public VerificarPagoHttpAdapter(
      @Value("${integraciones.pagos.verificador-url:}") String url,
      @Value("${integraciones.pagos.api-token:}") String token,
      @Value("${integraciones.pagos.webhook-secret:}") String secret) {
    this.url = url;
    this.webhookSecret = secret;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    client =
        RestClient.builder()
            .requestFactory(factory)
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
  }

  public PagoVerificado verificar(PasarelaProveedor proveedor, String id, String credencial) {
    if (webhookSecret.isBlank()
        || credencial == null
        || !MessageDigest.isEqual(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            credencial.getBytes(StandardCharsets.UTF_8)))
      throw new AccessDeniedException("Webhook no autenticado");
    if (url.isBlank()) throw new BusinessException("Verificador de pagos no configurado");
    var pago =
        client
            .get()
            .uri(url + "/{proveedor}/transacciones/{id}", proveedor.name(), id)
            .retrieve()
            .body(PagoVerificado.class);
    if (pago == null
        || pago.obligacionPagoId() == null
        || pago.monto() == null
        || pago.metodo() == null)
      throw new BusinessException("Respuesta de verificación incompleta");
    return pago;
  }
}
