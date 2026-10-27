package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;

public record DocumentoSolicitudResponseDto(
    TipoDocumentoSolicitud tipo,
    EstadoValidacionDocumento estado,
    String observacion,
    java.util.UUID versionId,
    Boolean iaValida,
    Boolean dniApiCoincide,
    com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital estadoFirma,
    String detalleFirma,
    Boolean vinculoParentalCoincide) {}
