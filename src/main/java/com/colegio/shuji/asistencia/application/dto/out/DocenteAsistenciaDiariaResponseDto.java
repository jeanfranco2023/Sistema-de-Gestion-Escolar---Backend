package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import java.time.LocalDate;
import java.time.LocalTime;

public record DocenteAsistenciaDiariaResponseDto(
    Long id,
    Long matriculaId,
    String estudiante,
    LocalDate fechaSesion,
    LocalTime horaRegistro,
    EstadoAsistenciaAula estado) {}
