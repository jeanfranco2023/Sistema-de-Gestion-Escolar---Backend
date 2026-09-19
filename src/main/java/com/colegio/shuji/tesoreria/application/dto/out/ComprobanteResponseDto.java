package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ComprobanteResponseDto(
    Long id,
    UUID uuid,
    Long pagoTransaccionId,
    TipoComprobante tipoComprobante,
    String serie,
    Integer correlativo,
    OffsetDateTime fechaEmision,
    BigDecimal montoTotal,
    EstadoComprobante estadoComprobante,
    OffsetDateTime fechaAnulacion,
    String motivoAnulacion,
    String urlPdfComprobante) {}
