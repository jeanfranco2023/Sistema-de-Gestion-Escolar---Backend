package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MarcaBiometricaRequestDto(
    @NotBlank @Pattern(regexp = "[0-9]{8}") String dniLeido,
    @NotNull OffsetDateTime fechaHora,
    @NotBlank @Size(max = 30) String dispositivoCodigo) {}
