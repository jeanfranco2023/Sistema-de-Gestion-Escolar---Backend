package com.colegio.shuji.evaluacion.application.dto.out;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionRefuerzoResponseDto(
    Long id,
    Short anioLectivoId,
    Short periodoAcademicoId,
    Short areaCurricularId,
    Long docenteUsuarioId,
    String tema,
    LocalDate fechaProgramada,
    LocalTime horaInicio,
    LocalTime horaFin,
    String aulaAsignada) {}
