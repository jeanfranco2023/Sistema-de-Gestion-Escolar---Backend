package com.colegio.shuji.evaluacion.application.dto.in;

import com.colegio.shuji.evaluacion.domain.enums.*;
import jakarta.validation.constraints.*;

public record RegistrarAsistenciaRefuerzoRequestDto(
    @NotNull Long inscripcionId,
    @NotNull EstadoAsistenciaRefuerzo estadoAsistencia,
    @Size(max = 4000) String observaciones) {}
