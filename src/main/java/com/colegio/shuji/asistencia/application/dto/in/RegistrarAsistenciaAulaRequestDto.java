package com.colegio.shuji.asistencia.application.dto.in;

import com.colegio.shuji.asistencia.domain.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public record RegistrarAsistenciaAulaRequestDto(
    @NotNull Long matriculaId,
    @NotNull @PastOrPresent LocalDate fechaSesion,
    @NotNull LocalTime horaRegistro,
    @NotNull EstadoAsistenciaAula estado) {}
