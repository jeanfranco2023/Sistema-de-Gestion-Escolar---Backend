package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JustificarInasistenciaRequestDto(
    @NotNull Long asistenciaId,
    @NotBlank @Size(max = 4000) String motivo,
    @Size(max = 255) String documentoSustentoUrl) {}
