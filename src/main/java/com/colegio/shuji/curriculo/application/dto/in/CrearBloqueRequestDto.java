package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record CrearBloqueRequestDto(
    @NotNull @Min(1) Short numeroBloque,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @NotNull Boolean esRecreo) {}
