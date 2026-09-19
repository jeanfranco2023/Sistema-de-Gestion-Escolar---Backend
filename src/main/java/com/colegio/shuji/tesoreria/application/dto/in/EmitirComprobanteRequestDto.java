package com.colegio.shuji.tesoreria.application.dto.in;

import com.colegio.shuji.tesoreria.domain.enums.*;
import jakarta.validation.constraints.*;

public record EmitirComprobanteRequestDto(
    @NotNull Long pagoId,
    @NotNull TipoComprobante tipoComprobante,
    @NotBlank @Pattern(regexp = "[BE][0-9]{3}") String serie) {}
