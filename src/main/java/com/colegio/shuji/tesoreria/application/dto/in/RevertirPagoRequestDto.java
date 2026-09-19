package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevertirPagoRequestDto(
    @NotNull Long pagoId, @NotBlank @Size(max = 255) String motivo) {}
