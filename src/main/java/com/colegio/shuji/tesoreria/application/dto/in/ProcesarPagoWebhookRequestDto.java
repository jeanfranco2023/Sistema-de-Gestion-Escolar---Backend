package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.*;

public record ProcesarPagoWebhookRequestDto(
    @NotBlank @Size(max = 100) String transaccionId, @NotNull Long obligacionPagoId) {}
