package com.colegio.shuji.matricula.application.dto.out;

import java.math.BigDecimal;

public record PagoMatriculaPublicaResponseDto(
    String preferenceId, String enlacePago, BigDecimal monto, String moneda) {}
