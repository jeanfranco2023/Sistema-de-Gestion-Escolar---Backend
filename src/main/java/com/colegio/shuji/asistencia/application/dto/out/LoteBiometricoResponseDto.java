package com.colegio.shuji.asistencia.application.dto.out;

import java.time.*;

public record LoteBiometricoResponseDto(
    Long id,
    String nombreArchivo,
    Integer totalFilas,
    Integer marcasValidas,
    Integer marcasErroneas,
    Long importadoPorUsuarioId,
    OffsetDateTime createdAt) {}
