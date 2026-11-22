package com.colegio.shuji.asistencia.application.support;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utilitario para calcular la huella SHA-256 determinista de un lote biométrico. */
public final class HuellaImportacionBiometrica {
  private HuellaImportacionBiometrica() {}

  public static String calcular(ImportarLoteBiometricoRequestDto request) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (var marca : request.marcas()) {
        String fila = marca.dniLeido() + '|' + marca.fechaHora().toInstant() + '|'
            + marca.dispositivoCodigo() + '\n';
        digest.update(fila.getBytes(StandardCharsets.UTF_8));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no disponible", ex);
    }
  }
}
