package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import java.time.LocalDate;
import java.time.LocalTime;

public record AsistenciaAulaResponseDto(
    Long id,
    Long matriculaId,
    String nombreEstudiante,
    LocalDate fechaSesion,
    LocalTime horaRegistro,
    EstadoAsistenciaAula estado,
    Long auxiliarUsuarioId,
    Boolean justificada,
    String motivoJustificacion,
    boolean tieneDocumentoSustento,
    Integer seccionId,
    String seccion) {

  public AsistenciaAulaResponseDto(
      Long id,
      Long matriculaId,
      String nombreEstudiante,
      LocalDate fechaSesion,
      LocalTime horaRegistro,
      EstadoAsistenciaAula estado,
      Long auxiliarUsuarioId,
      Boolean justificada,
      String motivoJustificacion,
      boolean tieneDocumentoSustento) {
    this(
        id,
        matriculaId,
        nombreEstudiante,
        fechaSesion,
        horaRegistro,
        estado,
        auxiliarUsuarioId,
        justificada,
        motivoJustificacion,
        tieneDocumentoSustento,
        null,
        null);
  }
}
