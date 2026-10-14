package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;

public interface GeminiDocumentoMatriculaPort {
  record Resultado(boolean tipoCorrecto, boolean legible, boolean identidadCoincide, String observacion, boolean disponible) {
    public Resultado(boolean tipoCorrecto, boolean legible, boolean identidadCoincide, String observacion) {
      this(tipoCorrecto, legible, identidadCoincide, observacion, true);
    }
    public boolean valido() {
      return disponible && tipoCorrecto && legible && identidadCoincide;
    }
  }

  Resultado validar(
      TipoDocumentoSolicitud tipo,
      byte[] contenido,
      String mimeType,
      String nombresDeclarados,
      String documentoDeclarado);
}
