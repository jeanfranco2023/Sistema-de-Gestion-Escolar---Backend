package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.*;

public record ConfirmarMatriculaRequestDto(@NotNull Long matriculaId) {}
