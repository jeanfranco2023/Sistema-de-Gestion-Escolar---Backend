package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ImportarLoteBiometricoRequestDto(
    @NotBlank @Size(max = 150) String nombreArchivo,
    @NotEmpty @Size(max = 10000) List<@Valid MarcaBiometricaRequestDto> marcas) {}
