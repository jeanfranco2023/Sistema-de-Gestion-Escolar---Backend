package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.NotNull;

public record CrearPreferenciaMercadoPagoDto(
    @NotNull(message = "El ID de obligación es obligatorio") Long obligacionPagoId,
    String backUrlSuccess,
    String backUrlFailure) {}
