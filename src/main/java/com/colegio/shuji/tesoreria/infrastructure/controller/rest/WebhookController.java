package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.ProcesarPagoWebhookRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.application.port.in.ProcesarPagoPasarelaUseCase;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos/webhook")
@Validated
@RequiredArgsConstructor
@Tag(name = "Tesoreria")
public class WebhookController {
  private final ProcesarPagoPasarelaUseCase pagos;
  private final MercadoPagoWebhookSignatureValidator firmaMercadoPago;

  @PostMapping("/culqi")
  @Operation(summary = "pagos.procesarWebhook")
  public TransaccionResponseDto webhookCulqi(
      @Valid @RequestBody ProcesarPagoWebhookRequestDto r,
      @RequestHeader(value = "X-Webhook-Secret", required = false) String credencial) {
    return pagos.procesarWebhook(PasarelaProveedor.CULQI, r, credencial);
  }

  @PostMapping("/niubiz")
  @Operation(summary = "pagos.procesarWebhook")
  public TransaccionResponseDto webhookNiubiz(
      @Valid @RequestBody ProcesarPagoWebhookRequestDto r,
      @RequestHeader(value = "X-Webhook-Secret", required = false) String credencial) {
    return pagos.procesarWebhook(PasarelaProveedor.NIUBIZ, r, credencial);
  }

  @PostMapping("/mercadopago")
  @Operation(summary = "Procesar IPN / Webhook de Mercado Pago")
  public ResponseEntity<Void> webhookMercadoPago(
      @RequestParam(value = "data.id", required = false) String dataIdParam,
      @RequestParam(value = "dataId", required = false) String dataId,
      @RequestParam(value = "id", required = false) String id,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "topic", required = false) String topic,
      @RequestHeader(value = "x-signature", required = false) String signature,
      @RequestHeader(value = "x-request-id", required = false) String requestId,
      @RequestBody(required = false) Map<String, Object> body) {
    String transaccionId = dataId != null ? dataId : (dataIdParam != null ? dataIdParam : id);
    if (transaccionId == null && body != null && body.containsKey("data")) {
      Object dataObject = body.get("data");
      if (dataObject instanceof Map<?, ?> data && data.get("id") != null) {
        transaccionId = data.get("id").toString();
      }
    }
    if (transaccionId == null && body != null && body.get("id") != null) {
      transaccionId = body.get("id").toString();
    }

    if (!firmaMercadoPago.estaConfigurado()) {
      return ResponseEntity.status(503).build();
    }
    if (!firmaMercadoPago.validar(signature, requestId, transaccionId)) {
      return ResponseEntity.status(401).build();
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
    return ResponseEntity.ok().build();
  }
}
