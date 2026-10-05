package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ComprobanteResponseDto(
    Long id,
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
