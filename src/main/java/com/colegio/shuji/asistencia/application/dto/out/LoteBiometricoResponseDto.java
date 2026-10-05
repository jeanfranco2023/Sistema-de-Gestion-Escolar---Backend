package com.colegio.shuji.asistencia.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record LoteBiometricoResponseDto(
    Long id,
    String nombreArchivo,
    Integer totalFilas,
    Integer marcasValidas,
    Integer marcasErroneas,
    Long importadoPorUsuarioId,
    @JsonProperty("createdAt") OffsetDateTime fechaCreacion) {}
