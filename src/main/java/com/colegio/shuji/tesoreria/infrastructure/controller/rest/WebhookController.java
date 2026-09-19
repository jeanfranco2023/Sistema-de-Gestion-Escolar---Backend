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
      @RequestParam(value = "data.id", required = false) String dataId,
      @RequestParam(value = "id", required = false) String id,
      @RequestParam(value = "type", required = false) String type,
      @RequestBody(required = false) java.util.Map<String, Object> body) {
    String transaccionId = dataId != null ? dataId : id;
    if (transaccionId == null && body != null && body.containsKey("data")) {
      var data = (java.util.Map<?, ?>) body.get("data");
      if (data != null && data.get("id") != null) transaccionId = data.get("id").toString();
    }
    if (transaccionId != null
        && ("payment".equalsIgnoreCase(type)
            || (body != null && "payment.created".equals(body.get("action"))))) {
      pagos.procesarWebhook(
          PasarelaProveedor.MERCADO_PAGO,
          new ProcesarPagoWebhookRequestDto(transaccionId, null),
          "");
    }
    return org.springframework.http.ResponseEntity.ok().build();
  }
}
