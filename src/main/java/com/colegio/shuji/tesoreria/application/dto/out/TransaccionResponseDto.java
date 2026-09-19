package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TransaccionResponseDto(
    Long id,
    UUID uuid,
    Long obligacionPagoId,
    PasarelaProveedor pasarelaProveedor,
    String pasarelaTransaccionId,
    MetodoPago metodoPago,
    BigDecimal montoPagado,
    OffsetDateTime fechaPago,
    EstadoPago estadoPago,
    Map<String, Object> payloadWebhook) {}
