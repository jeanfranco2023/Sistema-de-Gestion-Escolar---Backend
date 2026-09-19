package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MercadoPagoHttpAdapter implements MercadoPagoPort {

  private final String apiUrl;
  private final String accessToken;
  private final String publicKey;
  private final Set<String> returnUrlHosts;
  private final RestClient client;

  public MercadoPagoHttpAdapter(
      @Value("${integraciones.pagos.mercadopago.api-url:https://api.mercadopago.com}") String apiUrl,
      @Value("${integraciones.pagos.mercadopago.access-token:}") String accessToken,
      @Value("${integraciones.pagos.mercadopago.public-key:APP_USR-86474939-d202-45ae-a7f3-eaeeb7b5606a}") String publicKey,
      @Value("${integraciones.pagos.mercadopago.return-url-hosts:}") String returnUrlHosts) {
    this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    this.accessToken = accessToken;
    this.publicKey = publicKey;
    this.returnUrlHosts =
        java.util.Arrays.stream(returnUrlHosts.split(","))
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .map(v -> v.toLowerCase(java.util.Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(10));
    this.client = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public PreferenciaMercadoPagoResponseDto crearPreferencia(
      ObligacionPago obligacion, String descripcion, String backUrlSuccess, String backUrlFailure) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new BusinessException("Mercado Pago no configurado (access token ausente)");
    }

    Map<String, Object> item = new HashMap<>();
    item.put("title", descripcion);
    item.put("quantity", 1);
    item.put("unit_price", obligacion.saldo());
    item.put("currency_id", "PEN");

    Map<String, Object> body = new HashMap<>();
    body.put("items", List.of(item));
    body.put("external_reference", obligacion.getId().toString());
    body.put("auto_return", "approved");

    validarUrlSegura(backUrlSuccess);
    validarUrlSegura(backUrlFailure);

    Map<String, String> backUrls = new HashMap<>();
    if (backUrlSuccess != null && !backUrlSuccess.isBlank()) {
      backUrls.put("success", backUrlSuccess);
    }
    if (backUrlFailure != null && !backUrlFailure.isBlank()) {
      backUrls.put("failure", backUrlFailure);
    }
    if (!backUrls.isEmpty()) {
      body.put("back_urls", backUrls);
    }

    var response =
        client
            .post()
            .uri(apiUrl + "/checkout/preferences")
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Idempotency-Key", obligacion.getId() + "-" + obligacion.saldo())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null || response.get("id") == null) {
      throw new BusinessException("Respuesta inválida al crear preferencia de Mercado Pago");
    }

    String preferenceId = response.get("id").toString();
    String initPoint = response.get("init_point") != null ? response.get("init_point").toString() : "";
    String sandboxInitPoint =
        response.get("sandbox_init_point") != null ? response.get("sandbox_init_point").toString() : "";

    return new PreferenciaMercadoPagoResponseDto(preferenceId, initPoint, sandboxInitPoint, publicKey);
  }

  @Override
  public VerificarPagoPort.PagoVerificado consultarPago(String paymentId) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new BusinessException("Mercado Pago no configurado (access token ausente)");
    }

    var response =
        client
            .get()
            .uri(apiUrl + "/v1/payments/{paymentId}", paymentId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null || response.get("external_reference") == null) {
      throw new BusinessException("Respuesta incompleta de Mercado Pago para el pago: " + paymentId);
    }

    Long obligacionPagoId = Long.valueOf(response.get("external_reference").toString());

    BigDecimal monto = BigDecimal.ZERO;
    Object amountObj = response.get("transaction_amount");
    if (amountObj instanceof Number num) {
      monto = BigDecimal.valueOf(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    } else if (amountObj instanceof String str) {
      monto = new BigDecimal(str).setScale(2, RoundingMode.HALF_UP);
    }

    String currencyId =
        response.get("currency_id") != null ? response.get("currency_id").toString() : "PEN";

    String paymentMethodId =
        response.get("payment_method_id") != null ? response.get("payment_method_id").toString() : "";

    MetodoPago metodo = MetodoPago.TARJETA_CREDITO;
    if (!paymentMethodId.isBlank()) {
      String pm = paymentMethodId.toLowerCase();
      if (pm.contains("yape")) {
        metodo = MetodoPago.YAPE;
      } else if (pm.contains("plin")) {
        metodo = MetodoPago.PLIN;
      } else if (pm.contains("deb") || pm.contains("debit")) {
        metodo = MetodoPago.TARJETA_DEBITO;
      } else if (pm.contains("transf") || pm.contains("bank")) {
        metodo = MetodoPago.TRANSFERENCIA;
      } else if (pm.contains("cash") || pm.contains("efectivo")) {
        metodo = MetodoPago.EFECTIVO;
      } else {
        metodo = MetodoPago.TARJETA_CREDITO;
      }
    }

    String status = response.get("status") != null ? response.get("status").toString() : "";
    boolean aprobado = "approved".equalsIgnoreCase(status);

    return new VerificarPagoPort.PagoVerificado(
        paymentId, obligacionPagoId, monto, currencyId, metodo, aprobado);
  }

  private void validarUrlSegura(String url) {
    if (url == null || url.isBlank()) return;
    try {
      var uri = java.net.URI.create(url);
      var host = uri.getHost();
      if (!uri.isAbsolute()
          || host == null
          || uri.getUserInfo() != null
          || !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new BusinessException("La URL de retorno debe ser HTTPS, absoluta y sin credenciales");
      }
      if (!returnUrlHosts.contains(host.toLowerCase(java.util.Locale.ROOT))) {
        throw new BusinessException("Dominio no permitido en URL de retorno");
      }
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("URL de retorno inválida");
    }
  }
}
