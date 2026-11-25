package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReasignarDocenteRequestDto(@NotNull @Positive Long docenteUsuarioId) {}
