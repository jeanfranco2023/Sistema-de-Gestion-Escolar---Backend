package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.*;

public record RevertirPagoRequestDto(
    @NotNull Long pagoId, @NotBlank @Size(max = 255) String motivo) {}
