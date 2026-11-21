package com.colegio.shuji.evaluacion.application.dto.out;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionRefuerzoDetalleResponseDto(
    Long id,
    Short anioLectivoId,
    Short periodoAcademicoId,
    String periodoNombre,
    Short areaCurricularId,
    String areaNombre,
    Long docenteUsuarioId,
    String docenteNombre,
    String tema,
    LocalDate fechaProgramada,
    LocalTime horaInicio,
    LocalTime horaFin,
    String aulaAsignada,
    Integer seccionId,
    String nivelNombre,
    String gradoNombre,
    String seccionLetra,
    String gradoSeccion,
    long totalInscritos,
    long pendientes,
    long asistieron,
    long faltaron,
    long justificados,
    long casosC,
    long casosB,
    String estadoSesion) {}
