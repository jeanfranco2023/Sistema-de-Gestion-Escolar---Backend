package com.colegio.shuji.tesoreria.application.dto.out;

import java.math.BigDecimal;
import java.util.*;

public record EstadoCuentaEstudianteResponseDto(
    Long estudianteId, List<ObligacionResponseDto> obligaciones, BigDecimal saldoTotal) {}
