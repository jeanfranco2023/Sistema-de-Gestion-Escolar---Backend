package com.colegio.shuji.matricula.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import java.time.OffsetDateTime;

public record MatriculaResponseDto(
    Long id,
    Short anioLectivoId,
    Long estudianteId,
    Integer seccionId,
    OffsetDateTime fechaMatricula,
    EstadoMatricula estadoMatricula,
    @JsonProperty("reservaExpiraAt") OffsetDateTime fechaExpiracionReserva,
    String observaciones) {}
