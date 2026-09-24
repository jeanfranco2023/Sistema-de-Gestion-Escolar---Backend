package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
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
    Long matriculaId) {}
