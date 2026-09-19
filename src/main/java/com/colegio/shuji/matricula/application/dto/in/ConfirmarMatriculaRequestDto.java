package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.NotNull;

public record ConfirmarMatriculaRequestDto(@NotNull Long matriculaId) {}
