package com.colegio.shuji.tesoreria.application.dto.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO con los datos consolidados para emitir el voucher / comprobante de pago oficial de matrícula en PDF.
 */
public record ComprobanteMatriculaPdfDto(
    String numeroComprobante,
    String tipoComprobante,
    OffsetDateTime fechaEmision,
    String metodoPago,
    String transaccionId,
    String nombreApoderado,
    String dniApoderado,
    String emailApoderado,
    String celularApoderado,
    String direccionApoderado,
    String nombreEstudiante,
    String dniEstudiante,
    String nivelGradoSeccion,
    String codigoMatricula,
    BigDecimal montoTotal,
    String concepto
) {}
