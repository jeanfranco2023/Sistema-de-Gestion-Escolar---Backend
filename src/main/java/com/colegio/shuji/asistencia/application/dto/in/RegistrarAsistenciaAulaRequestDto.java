package com.colegio.shuji.asistencia.application.dto.in;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.LocalTime;

public record RegistrarAsistenciaAulaRequestDto(
    @NotNull Long matriculaId,
    @NotNull @PastOrPresent LocalDate fechaSesion,
    @NotNull LocalTime horaRegistro,
    @NotNull EstadoAsistenciaAula estado) {}
