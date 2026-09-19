package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record MarcaBiometricaRequestDto(
    @NotBlank @Pattern(regexp = "[0-9]{8}") String dniLeido,
    @NotNull OffsetDateTime fechaHora,
    @NotBlank @Size(max = 30) String dispositivoCodigo) {}
