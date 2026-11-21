package com.colegio.shuji.matricula.application.dto.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatriculaPresencialResponseDto(
    Long matriculaId,
    Long estudianteId,
    Long apoderadoId,
    UUID solicitudId,
    String comprobanteCodigo,
    BigDecimal montoCobrado,
    String metodoPago,
    OffsetDateTime fechaEmision,
    String usernameApoderado,
    String emailApoderado,
    boolean cuentaNueva,
    String voucherPdfBase64,
    String mensaje
) {}
