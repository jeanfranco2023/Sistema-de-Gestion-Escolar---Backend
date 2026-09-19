package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;
import com.colegio.shuji.tesoreria.application.port.in.*;
import com.colegio.shuji.tesoreria.domain.enums.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagos/webhook")
@Validated
@RequiredArgsConstructor
@Tag(name = "Tesoreria")
public class WebhookController {
  private final ProcesarPagoPasarelaUseCase pagos;

  @org.springframework.beans.factory.annotation.Value("${integraciones.pagos.mercadopago.webhook-secret:}")
  private String webhookSecret;

  @PostMapping("/culqi")
  @Operation(summary = "pagos.procesarWebhook")
  public TransaccionResponseDto operacion0(
      @Valid @RequestBody ProcesarPagoWebhookRequestDto r,
      @RequestHeader(value = "X-Webhook-Secret", required = false) String credencial) {
    return pagos.procesarWebhook(PasarelaProveedor.CULQI, r, credencial);
  }

  @PostMapping("/niubiz")
  @Operation(summary = "pagos.procesarWebhook")
  public TransaccionResponseDto operacion1(
      @Valid @RequestBody ProcesarPagoWebhookRequestDto r,
      @RequestHeader(value = "X-Webhook-Secret", required = false) String credencial) {
    return pagos.procesarWebhook(PasarelaProveedor.NIUBIZ, r, credencial);
  }

  @PostMapping("/mercadopago")
  @Operation(summary = "Procesar IPN / Webhook de Mercado Pago")
  public org.springframework.http.ResponseEntity<Void> webhookMercadoPago(
      @RequestParam(value = "data.id", required = false) String dataIdParam,
      @RequestParam(value = "dataId", required = false) String dataId,
      @RequestParam(value = "id", required = false) String id,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "topic", required = false) String topic,
      @RequestHeader(value = "x-signature", required = false) String signature,
      @RequestHeader(value = "x-request-id", required = false) String requestId,
      @RequestBody(required = false) java.util.Map<String, Object> body) {
    String transaccionId = dataId != null ? dataId : (dataIdParam != null ? dataIdParam : id);
    if (transaccionId == null && body != null && body.containsKey("data")) {
      var data = (java.util.Map<?, ?>) body.get("data");
      if (data != null && data.get("id") != null) transaccionId = data.get("id").toString();
    }
    if (transaccionId == null && body != null && body.get("id") != null) {
      transaccionId = body.get("id").toString();
    }

    if (webhookSecret != null && !webhookSecret.isBlank()) {
      if (signature == null || signature.isBlank() || !validarFirmaMercadoPago(signature, requestId, transaccionId)) {
        return org.springframework.http.ResponseEntity.status(401).build();
      }
    }

    String eventType = type != null ? type : topic;
    boolean esEventoPago =
        "payment".equalsIgnoreCase(eventType)
            || "payment.created".equalsIgnoreCase(eventType)
            || "payment.updated".equalsIgnoreCase(eventType)
            || (body != null
                && ("payment".equalsIgnoreCase(String.valueOf(body.get("type")))
                    || "payment.created".equalsIgnoreCase(String.valueOf(body.get("action")))
                    || "payment.updated".equalsIgnoreCase(String.valueOf(body.get("action")))));

    if (transaccionId != null && esEventoPago) {
      pagos.procesarWebhook(
          PasarelaProveedor.MERCADO_PAGO,
          new ProcesarPagoWebhookRequestDto(transaccionId, null),
          "");
    }
    return org.springframework.http.ResponseEntity.ok().build();
  }

  private boolean validarFirmaMercadoPago(String signature, String requestId, String transaccionId) {
    String ts = null;
    String v1 = null;
    for (String part : signature.split(",")) {
      String[] kv = part.trim().split("=", 2);
      if (kv.length == 2) {
        if ("ts".equalsIgnoreCase(kv[0].trim())) {
          ts = kv[1].trim();
        } else if ("v1".equalsIgnoreCase(kv[0].trim())) {
          v1 = kv[1].trim();
        }
      }
    }
    if (ts == null || v1 == null) {
      return false;
    }
    try {
      javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
      hmac.init(
          new javax.crypto.spec.SecretKeySpec(
              webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));

      String reqIdVal = (requestId != null) ? requestId : "";
      String txIdVal = (transaccionId != null) ? transaccionId : "";

      String manifest = "id:" + txIdVal + ";request-id:" + reqIdVal + ";ts:" + ts + ";";
      byte[] hash = hmac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      if (bytesToHex(hash).equalsIgnoreCase(v1)) {
        return true;
      }
      if (reqIdVal.isEmpty()) {
        String altManifest = "id:" + txIdVal + ";ts:" + ts + ";";
        byte[] altHash = hmac.doFinal(altManifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (bytesToHex(altHash).equalsIgnoreCase(v1)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
