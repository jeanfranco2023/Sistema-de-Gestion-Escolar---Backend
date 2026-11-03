package com.colegio.shuji.matricula.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EstudianteResponseDto(
    Long id,
    String codigoInterno,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    LocalDate fechaNacimiento,
    Genero genero,
    String codigoEstudianteSiagie,
    Boolean validadoReniec,
    OrigenRegistro origenRegistro,
    String grupoSanguineo,
    String alergiasCondiciones,
    Boolean activo,
    String estado,
    @JsonProperty("createdAt") OffsetDateTime fechaCreacion) {

  public EstudianteResponseDto {
    codigoInterno = generarCodigoInterno(id);
    if (estado == null) {
      estado = Boolean.TRUE.equals(activo) ? "ACTIVO" : "INACTIVO";
    }
  }

  public EstudianteResponseDto(
      Long id,
      String codigoInterno,
      TipoDocumento tipoDocumento,
      String numeroDocumento,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      LocalDate fechaNacimiento,
      Genero genero,
      String codigoEstudianteSiagie,
      Boolean validadoReniec,
      OrigenRegistro origenRegistro,
      String grupoSanguineo,
      String alergiasCondiciones,
      Boolean activo,
      OffsetDateTime fechaCreacion) {
    this(id, codigoInterno, tipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno,
         fechaNacimiento, genero, codigoEstudianteSiagie, validadoReniec, origenRegistro,
         grupoSanguineo, alergiasCondiciones, activo,
         Boolean.TRUE.equals(activo) ? "ACTIVO" : "INACTIVO", fechaCreacion);
  }

  public static String generarCodigoInterno(Long estudianteId) {
    return estudianteId == null ? null : "ALU-" + String.format("%06d", estudianteId);
  }
}
