package com.colegio.shuji.tesoreria.application.dto.in;

import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarPagoCajaRequestDto(
    @NotNull Long obligacionPagoId,
    @NotBlank @Size(max = 100) String pasarelaTransaccionId,
    @NotNull MetodoPago metodoPago,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal montoPagado) {}
