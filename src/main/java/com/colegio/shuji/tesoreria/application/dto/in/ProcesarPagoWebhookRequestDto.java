package com.colegio.shuji.tesoreria.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcesarPagoWebhookRequestDto(
    @NotBlank @Size(max = 100) String transaccionId, @NotNull Long obligacionPagoId) {}
