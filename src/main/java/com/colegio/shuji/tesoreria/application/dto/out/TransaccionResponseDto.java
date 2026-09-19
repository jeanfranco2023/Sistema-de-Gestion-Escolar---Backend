package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

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
