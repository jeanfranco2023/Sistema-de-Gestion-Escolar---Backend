package com.colegio.shuji.shared.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.shared.infrastructure.storage.LocalPrivateFileStore;
import com.colegio.shuji.usuario.infrastructure.adapter.LocalExpedienteEmpleadoStorageAdapter;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalExpedienteEmpleadoStorageAdapterTest {

  private static final byte[] SIGNING_KEY = new byte[32];

  @TempDir Path tempDir;

  @Test
  void persistePdfYGeneraEnlacePrivadoDeDescarga() {
    Arrays.fill(SIGNING_KEY, (byte) 11);
    var store = new LocalPrivateFileStore(
        tempDir.resolve("private"),
        "http://localhost:10000",
        60,
        SIGNING_KEY,
        Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC));
    var adapter = new LocalExpedienteEmpleadoStorageAdapter(store);
    String key = "expedientes/17/cv/documento.pdf";
    byte[] pdf = "%PDF-expediente-privado".getBytes(StandardCharsets.UTF_8);

    adapter.guardar(key, pdf);
    Map<String, String> url = parseUrl(adapter.urlFirmada(key));
    var descargado = store.descargar(
        url.get("bucket"), url.get("key"), Long.parseLong(url.get("expires")), url.get("sig"));

    assertArrayEquals(pdf, descargado.bytes());
    assertTrue(java.nio.file.Files.exists(
        tempDir.resolve("private/expedientes-empleados/expedientes/17/cv/documento.pdf")));
  }

  private static Map<String, String> parseUrl(String value) {
    String query = URI.create(value).getRawQuery();
    Map<String, String> result = new HashMap<>();
    for (String pair : query.split("&")) {
      String[] parts = pair.split("=", 2);
      result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
    }
    return result;
  }
}
