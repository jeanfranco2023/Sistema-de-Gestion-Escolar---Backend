package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record MatriculaResponseDto(
    Long id,
    Short anioLectivoId,
    Long estudianteId,
    Integer seccionId,
    OffsetDateTime fechaMatricula,
    EstadoMatricula estadoMatricula,
    @JsonProperty("reservaExpiraAt") OffsetDateTime fechaExpiracionReserva,
    String observaciones,
    EstadoPagoMatriculaFicha estadoPagoMatricula,
    ModalidadIngreso modalidadIngreso,
    String colegioProcedencia) {

  public MatriculaResponseDto(
      Long id,
      Short anioLectivoId,
      Long estudianteId,
      Integer seccionId,
      OffsetDateTime fechaMatricula,
      EstadoMatricula estadoMatricula,
      OffsetDateTime fechaExpiracionReserva,
      String observaciones,
      EstadoPagoMatriculaFicha estadoPagoMatricula) {
    this(id, anioLectivoId, estudianteId, seccionId, fechaMatricula, estadoMatricula,
        fechaExpiracionReserva, observaciones, estadoPagoMatricula, ModalidadIngreso.REGULAR, null);
  }
}
