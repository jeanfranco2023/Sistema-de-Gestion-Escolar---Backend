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
  private final com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoWebhookSignatureValidator
      firmaMercadoPago;

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
      Object dataObject = body.get("data");
      if (dataObject instanceof java.util.Map<?, ?> data && data.get("id") != null) {
        transaccionId = data.get("id").toString();
      }
    }
    if (transaccionId == null && body != null && body.get("id") != null) {
      transaccionId = body.get("id").toString();
    }

    if (firmaMercadoPago.estaConfigurado()) {
      if (!firmaMercadoPago.validar(signature, requestId, transaccionId)) {
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

}
