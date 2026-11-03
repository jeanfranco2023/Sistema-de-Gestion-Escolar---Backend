package com.colegio.shuji.matricula.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import java.time.OffsetDateTime;

public record ApoderadoResponseDto(
    Long id,
    Long usuarioId,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String celular,
    String email,
    String direccion,
    String ubigeoInei,
    Boolean validadoReniec,
    OrigenRegistro origenRegistro,
    Boolean activo,
    String estado,
    @JsonProperty("createdAt") OffsetDateTime fechaCreacion) {

  public ApoderadoResponseDto {
    if (activo == null) activo = true;
    if (estado == null) estado = Boolean.TRUE.equals(activo) ? "ACTIVO" : "INACTIVO";
  }

  public ApoderadoResponseDto(
      Long id,
      Long usuarioId,
      TipoDocumento tipoDocumento,
      String numeroDocumento,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      String celular,
      String email,
      String direccion,
      String ubigeoInei,
      Boolean validadoReniec,
      OrigenRegistro origenRegistro,
      OffsetDateTime fechaCreacion) {
    this(id, usuarioId, tipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno,
         celular, email, direccion, ubigeoInei, validadoReniec, origenRegistro,
         true, "ACTIVO", fechaCreacion);
  }
}
