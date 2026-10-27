package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.domain.enums.Parentesco;

public interface GeminiDocumentoMatriculaPort {
  record ContextoParental(String nombres, String dni, Parentesco parentesco) {}

  record Resultado(boolean tipoCorrecto, boolean legible, boolean identidadCoincide, String observacion,
      boolean disponible, Boolean vinculoParentalCoincide) {
    public Resultado(boolean tipoCorrecto, boolean legible, boolean identidadCoincide, String observacion, boolean disponible) {
      this(tipoCorrecto, legible, identidadCoincide, observacion, disponible, null);
    }
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
      String documentoDeclarado,
      ContextoParental progenitor);
}
