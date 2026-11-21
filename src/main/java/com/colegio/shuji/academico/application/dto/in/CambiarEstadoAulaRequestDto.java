package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAulaRequestDto(
    @NotNull(message = "El estado activa es obligatorio")
    Boolean activa
) {}
