package com.colegio.shuji.curriculo.application.dto.out;

import java.time.*;

public record BloqueHorarioResponseDto(
    Short id, Short numeroBloque, LocalTime horaInicio, LocalTime horaFin, Boolean esRecreo) {}
