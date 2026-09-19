package com.colegio.shuji.tesoreria.application.dto.in;

import com.colegio.shuji.tesoreria.domain.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RegistrarPagoCajaRequestDto(
    @NotNull Long obligacionPagoId,
    @NotBlank @Size(max = 100) String pasarelaTransaccionId,
    @NotNull MetodoPago metodoPago,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal montoPagado) {}
