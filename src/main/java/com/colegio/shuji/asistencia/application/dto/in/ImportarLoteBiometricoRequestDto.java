package com.colegio.shuji.asistencia.application.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record ImportarLoteBiometricoRequestDto(
    @NotBlank @Size(max = 150) String nombreArchivo,
    @NotEmpty @Size(max = 10000) List<@Valid MarcaBiometricaRequestDto> marcas) {}
