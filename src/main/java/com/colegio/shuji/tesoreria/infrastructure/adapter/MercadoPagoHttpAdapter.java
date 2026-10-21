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
  private final String matriculaReturnUrl;
  private final RestClient client;
  @Value("${integraciones.pagos.mercadopago.sandbox:true}")
  private boolean sandbox = true;
  @Value("${integraciones.pagos.mercadopago.notification-url:}")
  private String notificationUrl = "";
  private volatile Boolean cuentaDePrueba;
  private String cuentaId;

  public MercadoPagoHttpAdapter(
      @Value("${integraciones.pagos.mercadopago.api-url:https://api.mercadopago.com}") String apiUrl,
      @Value("${integraciones.pagos.mercadopago.access-token:}") String accessToken,
      @Value("${integraciones.pagos.mercadopago.public-key:}") String publicKey,
      @Value("${integraciones.pagos.mercadopago.return-url-hosts:}") String returnUrlHosts,
      @Value("${integraciones.pagos.mercadopago.matricula-return-url:}") String matriculaReturnUrl) {
    this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    this.accessToken = accessToken;
    this.publicKey = publicKey;
    this.returnUrlHosts =
        java.util.Arrays.stream(returnUrlHosts.split(","))
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .map(v -> v.toLowerCase(java.util.Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    this.matriculaReturnUrl = matriculaReturnUrl == null ? "" : matriculaReturnUrl.trim();
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(10));
    this.client = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public PreferenciaMercadoPagoResponseDto crearPreferencia(
      ObligacionPago obligacion, String descripcion, String backUrlSuccess, String backUrlFailure) {
    if (backUrlSuccess == null || backUrlSuccess.isBlank()) backUrlSuccess = matriculaReturnUrl;
    if (backUrlFailure == null || backUrlFailure.isBlank()) backUrlFailure = matriculaReturnUrl;
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
    agregarNotificacion(body);

    validarUrlSegura(backUrlSuccess);
    validarUrlSegura(backUrlFailure);
    validarAmbiente();

    Map<String, String> backUrls = new HashMap<>();
    if (backUrlSuccess != null && !backUrlSuccess.isBlank()) {
      backUrls.put("success", backUrlSuccess);
      body.put("auto_return", "approved");
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

    return new PreferenciaMercadoPagoResponseDto(preferenceId, enlaceCheckout(initPoint, sandboxInitPoint), sandboxInitPoint, publicKey);
  }

  @Override
  public PreferenciaMercadoPagoResponseDto crearPreferenciaMatriculaPublica(
      String referencia, BigDecimal monto, String descripcion) {
    validarAmbiente();
    Map<String, Object> item = new HashMap<>();
    item.put("title", descripcion);
    item.put("quantity", 1);
    item.put("unit_price", monto.setScale(2, RoundingMode.HALF_UP));
    item.put("currency_id", "PEN");

    Map<String, Object> body = new HashMap<>();
    body.put("items", List.of(item));
    body.put("external_reference", referencia);
    agregarNotificacion(body);
    body.put("statement_descriptor", "SHUJI MATRICULA");
    body.put("expires", true);
    body.put("expiration_date_from", java.time.OffsetDateTime.now().toString());
    body.put("expiration_date_to", java.time.OffsetDateTime.now().plusMinutes(30).toString());
    if (!matriculaReturnUrl.isBlank()) {
      validarUrlSegura(matriculaReturnUrl);
      body.put("back_urls", Map.of(
          "success", matriculaReturnUrl,
          "failure", matriculaReturnUrl,
          "pending", matriculaReturnUrl));
      body.put("auto_return", "approved");
    }

    var response =
        client
            .post()
            .uri(apiUrl + "/checkout/preferences")
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Idempotency-Key", referencia)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null || response.get("id") == null) {
      throw new BusinessException("Respuesta inválida al crear pago de matrícula");
    }
    String preferenceId = response.get("id").toString();
    String initPoint = response.get("init_point") == null ? "" : response.get("init_point").toString();
    String sandboxInitPoint = response.get("sandbox_init_point") == null
        ? "" : response.get("sandbox_init_point").toString();
    return new PreferenciaMercadoPagoResponseDto(preferenceId, enlaceCheckout(initPoint, sandboxInitPoint), sandboxInitPoint, publicKey);
  }

  @Override
  public VerificarPagoPort.PagoVerificado consultarPago(String paymentId) {
    if (paymentId == null || !paymentId.matches("[0-9]{1,32}"))
      throw new BusinessException("Identificador de pago inválido");
    if (accessToken == null || accessToken.isBlank()) {
      throw new BusinessException("Mercado Pago no configurado (access token ausente)");
    }

    validarAmbiente();
    var response =
        client
            .get()
            .uri(apiUrl + "/v1/payments/{paymentId}", paymentId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null || response.get("external_reference") == null
        || !paymentId.equals(String.valueOf(response.get("id")))
        || !(response.get("live_mode") instanceof Boolean live)) {
      throw new BusinessException("Respuesta incompleta de Mercado Pago para el pago: " + paymentId);
    }
    if (!cuentaId.equals(String.valueOf(response.get("collector_id"))))
      throw new BusinessException("El pago no pertenece al vendedor configurado");
    // Las cuentas test APP_USR también devuelven live_mode=true; se verifica su identidad en /users/me.
    if ((!sandbox && !live) || (sandbox && accessToken.startsWith("TEST-") && live))
      throw new BusinessException("El pago no corresponde al ambiente configurado");

    String referenciaExterna = response.get("external_reference").toString();
    Long obligacionPagoId = null;
    try {
      obligacionPagoId = Long.valueOf(referenciaExterna);
    } catch (NumberFormatException ignored) {
      // Las referencias no numéricas pertenecen a flujos ajenos a las obligaciones regulares.
    }

    BigDecimal monto;
    try {
      monto = new BigDecimal(String.valueOf(response.get("transaction_amount")))
          .setScale(2, RoundingMode.UNNECESSARY);
      if (monto.signum() <= 0) throw new NumberFormatException();
    } catch (ArithmeticException | NumberFormatException ex) {
      throw new BusinessException("Importe de pago inválido");
    }

    String currencyId =
        response.get("currency_id") != null ? response.get("currency_id").toString() : "";

    String paymentMethodId =
        response.get("payment_method_id") != null ? response.get("payment_method_id").toString() : "";

    MetodoPago metodo = switch (paymentMethodId.toLowerCase(java.util.Locale.ROOT)) {
      case "yape" -> MetodoPago.YAPE;
      case "plin" -> MetodoPago.PLIN;
      case "pagoefectivo_atm" -> MetodoPago.PAGO_EFECTIVO;
      default -> switch (String.valueOf(response.get("payment_type_id"))) {
        case "credit_card" -> MetodoPago.TARJETA_CREDITO;
        case "debit_card", "prepaid_card" -> MetodoPago.TARJETA_DEBITO;
        case "bank_transfer", "account_money" -> MetodoPago.TRANSFERENCIA;
        case "ticket", "atm" -> MetodoPago.EFECTIVO;
        default -> throw new BusinessException("Medio de pago no reconocido por la pasarela");
      };
    };

    String status = response.get("status") != null ? response.get("status").toString() : "";
    boolean aprobado = "approved".equalsIgnoreCase(status);

    return new VerificarPagoPort.PagoVerificado(
        paymentId, obligacionPagoId, monto, currencyId, metodo, aprobado, referenciaExterna);
  }

  @Override
  public java.util.Optional<VerificarPagoPort.PagoVerificado> buscarPagoAprobado(String referencia) {
    var response = client.get().uri(apiUrl + "/v1/payments/search?external_reference={ref}&status=approved&sort=date_created&criteria=desc&limit=10", referencia)
        .header("Authorization", "Bearer " + accessToken).retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    if (response == null || !(response.get("results") instanceof List<?> resultados))
      throw new BusinessException("No se pudo consultar el estado del pago");
    for (var resultado : resultados) {
      if (resultado instanceof Map<?, ?> pago && referencia.equals(pago.get("external_reference"))
          && pago.get("id") != null) {
        var verificado = consultarPago(pago.get("id").toString());
        if (verificado.aprobado() && referencia.equals(verificado.referenciaExterna()))
          return java.util.Optional.of(verificado);
      }
    }
    return java.util.Optional.empty();
  }

  private synchronized void validarAmbiente() {
    if (accessToken == null || accessToken.isBlank())
      throw new BusinessException("Mercado Pago no configurado (access token ausente)");
    if (cuentaDePrueba == null) {
      var cuenta = client.get().uri(apiUrl + "/users/me")
          .header("Authorization", "Bearer " + accessToken).retrieve()
          .body(new ParameterizedTypeReference<Map<String, Object>>() {});
      if (cuenta == null || !"PE".equals(cuenta.get("country_id"))
          || !String.valueOf(cuenta.get("id")).matches("[0-9]+"))
        throw new BusinessException("Se requiere una cuenta de Mercado Pago de Perú");
      cuentaId = String.valueOf(cuenta.get("id"));
      cuentaDePrueba = cuenta.get("tags") instanceof List<?> tags && tags.contains("test_user");
    }
    if (sandbox != cuentaDePrueba)
      throw new BusinessException("La cuenta de Mercado Pago no corresponde al ambiente configurado");
  }

  private String enlaceCheckout(String initPoint, String sandboxInitPoint) {
    String enlace = accessToken.startsWith("TEST-") ? sandboxInitPoint : initPoint;
    if (enlace == null || enlace.isBlank()) throw new BusinessException("Mercado Pago no devolvió el enlace de pago");
    var uri = java.net.URI.create(enlace);
    String host = uri.getHost();
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || host == null
        || !(host.equals("mercadopago.com.pe") || host.endsWith(".mercadopago.com.pe")
        || host.equals("mercadopago.com") || host.endsWith(".mercadopago.com")))
      throw new BusinessException("Enlace de Mercado Pago inválido");
    return enlace;
  }

  private void agregarNotificacion(Map<String, Object> body) {
    if (notificationUrl != null && !notificationUrl.isBlank()) {
      var uri = java.net.URI.create(notificationUrl);
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
        throw new BusinessException("El webhook debe ser una URL HTTPS pública");
      body.put("notification_url", notificationUrl);
    }
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
