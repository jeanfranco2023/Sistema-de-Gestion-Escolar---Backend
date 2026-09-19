package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.constraints.*;

public record JustificarInasistenciaRequestDto(
    @NotNull Long asistenciaId,
    @NotBlank @Size(max = 4000) String motivo,
    @Size(max = 255) String documentoSustentoUrl) {}
