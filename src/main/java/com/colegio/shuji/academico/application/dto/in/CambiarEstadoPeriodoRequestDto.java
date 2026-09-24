package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPeriodoRequestDto(@NotNull Boolean activo) {}
