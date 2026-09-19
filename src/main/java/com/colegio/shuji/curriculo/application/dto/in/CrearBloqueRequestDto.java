package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CrearBloqueRequestDto(
    @NotNull @Min(1) Short numeroBloque,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @NotNull Boolean esRecreo) {}
