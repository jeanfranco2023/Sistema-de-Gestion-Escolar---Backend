package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import java.util.UUID;

public interface AlmacenDocumentosMatriculaPort {
  record ArchivoGuardado(String bucket, String objectKey) {}

  void validarConfiguracion();

  ArchivoGuardado guardar(UUID solicitudId, TipoDocumentoSolicitud tipo, byte[] contenido, String mimeType);

  String urlFirmada(String bucket, String objectKey);
}
