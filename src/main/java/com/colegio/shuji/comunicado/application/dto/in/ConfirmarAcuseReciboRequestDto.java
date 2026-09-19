package com.colegio.shuji.comunicado.application.dto.in;

import jakarta.validation.constraints.*;

public record ConfirmarAcuseReciboRequestDto(@NotNull Boolean confirmarAcuse) {}
