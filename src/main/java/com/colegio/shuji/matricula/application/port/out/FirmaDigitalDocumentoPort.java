package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;

public interface FirmaDigitalDocumentoPort {
  record Resultado(EstadoFirmaDigital estado, String detalle, String certificadoSha256) {}
  Resultado verificar(byte[] contenido, String mimeType, boolean exigirEmisorReniec);
  default Resultado verificar(byte[] contenido, String mimeType) {
    return verificar(contenido, mimeType, false);
  }
}
