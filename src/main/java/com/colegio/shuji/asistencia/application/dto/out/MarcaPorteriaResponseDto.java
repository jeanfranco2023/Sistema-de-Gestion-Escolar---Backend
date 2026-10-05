package com.colegio.shuji.asistencia.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import java.time.OffsetDateTime;

public record MarcaPorteriaResponseDto(
    Long id,
    Long loteId,
    String dniLeido,
    OffsetDateTime fechaHora,
    String dispositivoCodigo,
    Long estudianteId,
    EstadoMarca estadoProcesamiento,
    @JsonProperty("createdAt") OffsetDateTime fechaCreacion) {}
