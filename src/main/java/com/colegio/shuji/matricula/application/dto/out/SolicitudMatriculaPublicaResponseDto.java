package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SolicitudMatriculaPublicaResponseDto(
    UUID id,
    String token,
    EstadoSolicitudMatriculaPublica estado,
    String nombreEstudiante,
    List<DocumentoSolicitudResponseDto> documentos,
    String enlacePago,
    BigDecimal montoPago,
    Long estudianteId,
    Long matriculaId,
    ModalidadIngreso modalidadIngreso,
    String colegioProcedencia) {

  public SolicitudMatriculaPublicaResponseDto(
      UUID id,
      String token,
      EstadoSolicitudMatriculaPublica estado,
      String nombreEstudiante,
      List<DocumentoSolicitudResponseDto> documentos,
      String enlacePago,
      BigDecimal montoPago,
      Long estudianteId,
      Long matriculaId) {
    this(id, token, estado, nombreEstudiante, documentos, enlacePago, montoPago,
        estudianteId, matriculaId, ModalidadIngreso.REGULAR, null);
  }
}
