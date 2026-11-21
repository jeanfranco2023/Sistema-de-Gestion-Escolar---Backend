package com.colegio.shuji.tesoreria.application.dto.in;

import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SimularPagoRequestDto(
    @NotNull MetodoPago metodoPago,
    @Size(max = 80) String referenciaOperacion) {}
