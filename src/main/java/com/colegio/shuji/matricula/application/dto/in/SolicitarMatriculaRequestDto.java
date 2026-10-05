package com.colegio.shuji.matricula.application.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record SolicitarMatriculaRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Long estudianteId,
    @NotNull Integer seccionId,
    @Future @JsonProperty("reservaExpiraAt") OffsetDateTime fechaExpiracionReserva,
    @Size(max = 500) String observaciones) {}
