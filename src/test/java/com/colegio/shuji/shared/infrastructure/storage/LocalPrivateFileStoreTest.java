package com.colegio.shuji.shared.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.asistencia.infrastructure.adapter.LocalStorageJustificacionAdapter;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.adapter.LocalStorageMatriculaAdapter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalPrivateFileStoreTest {
  private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
  private static final byte[] SIGNING_KEY = new byte[32];

  @TempDir Path tempDir;

  private LocalPrivateFileStore storage;

  @BeforeEach
  void setUp() {
    java.util.Arrays.fill(SIGNING_KEY, (byte) 7);
    storage = new LocalPrivateFileStore(
        tempDir.resolve("private"), "http://localhost:10000", 60, SIGNING_KEY,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void expedienteTieneLimiteDe10MbSinRelajarLos5MbDeOtrosBuckets() {
    byte[] sixMb = new byte[6 * 1024 * 1024];
    assertThrows(ResponseStatusException.class,
        () -> storage.guardar("matricula-documentos", "registro.pdf", sixMb));
    assertThrows(ResponseStatusException.class,
        () -> storage.guardar("asistencia-justificaciones", "registro.pdf", sixMb));

    storage.guardar("expedientes-empleados", "usuario/registro.pdf", sixMb);
    assertTrue(java.nio.file.Files.exists(
        tempDir.resolve("private/expedientes-empleados/usuario/registro.pdf")));

    byte[] overTenMb = new byte[10 * 1024 * 1024 + 1];
    assertThrows(ResponseStatusException.class,
        () -> storage.guardar("expedientes-empleados", "usuario/excede.pdf", overTenMb));
  }

  @Test
  void ambosPuertosGuardanYDescarganDesdeElStorePrivadoCompartido() {
    var matricula = new LocalStorageMatriculaAdapter(storage);
    var justification = new LocalStorageJustificacionAdapter(storage);
    byte[] matriculaPdf = "%PDF-matricula".getBytes(StandardCharsets.UTF_8);
    byte[] justificationPdf = "%PDF-justificacion".getBytes(StandardCharsets.UTF_8);

    var matriculaFile = matricula.guardar(
        UUID.fromString("b4a3a712-6f89-4d48-80c1-ec1d5b37cf22"),
        TipoDocumentoSolicitud.DNI_C4, matriculaPdf, "application/pdf");
    var justificationFile = justification.guardar(25L, justificationPdf);

    var matriculaDownload = parseUrl(matricula.urlFirmada(matriculaFile.bucket(), matriculaFile.objectKey()));
    var justificationDownload = parseUrl(justification.urlFirmada(justificationFile.clave()));
    assertArrayEquals(matriculaPdf, descargar(storage, matriculaDownload));
    assertArrayEquals(justificationPdf, descargar(storage, justificationDownload));
  }

  @Test
  void rutaDeDescargaDevuelveElArchivoSoloConLaUrlFirmada() throws Exception {
    String key = "b4a3a712-6f89-4d48-80c1-ec1d5b37cf22/dni_c4/3c52f7e4-cf2d-4a5e-b253-4818b3206a11.pdf";
    byte[] pdf = "%PDF-privado".getBytes(StandardCharsets.UTF_8);
    storage.guardar("matricula-documentos", key, pdf);
    Map<String, String> query = parseUrl(storage.urlFirmada("matricula-documentos", key));
    var mvc = MockMvcBuilders.standaloneSetup(new LocalStorageDownloadController(storage)).build();

    mvc.perform(get("/api/v1/storage/local/download")
            .param("bucket", query.get("bucket"))
            .param("key", query.get("key"))
            .param("expires", query.get("expires"))
            .param("sig", query.get("sig")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(content().bytes(pdf));
  }

  @Test
  void rechazaTraversalAntesDeCrearArchivosFueraDelRoot() {
    assertThrows(ResponseStatusException.class,
        () -> storage.guardar("matricula-documentos", "../escape.pdf", new byte[] {1}));
    assertThrows(ResponseStatusException.class,
        () -> storage.urlFirmada("matricula-documentos", "id/../../escape.pdf"));
    org.junit.jupiter.api.Assertions.assertFalse(java.nio.file.Files.exists(tempDir.resolve("escape.pdf")));
  }

  @Test
  void rechazaFirmaAlteradaYEnlaceVencido() {
    String key = "b4a3a712-6f89-4d48-80c1-ec1d5b37cf22/dni_c4/3c52f7e4-cf2d-4a5e-b253-4818b3206a11.pdf";
    storage.guardar("matricula-documentos", key, new byte[] {1, 2, 3});
    Map<String, String> query = parseUrl(storage.urlFirmada("matricula-documentos", key));
    String originalSignature = query.get("sig");
    String tamperedSignature = (originalSignature.startsWith("0") ? "1" : "0") + originalSignature.substring(1);

    assertThrows(ResponseStatusException.class,
        () -> storage.descargar("matricula-documentos", key, Long.parseLong(query.get("expires")), tamperedSignature));

    var expiredStorage = new LocalPrivateFileStore(
        tempDir.resolve("private"), "http://localhost:10000", 60, SIGNING_KEY,
        Clock.fixed(NOW.plusSeconds(61), ZoneOffset.UTC));
    assertThrows(ResponseStatusException.class,
        () -> expiredStorage.descargar(
            "matricula-documentos", key, Long.parseLong(query.get("expires")), originalSignature));
  }

  @Test
  void generaUrlHttpDeBackendConFirmaDeTamanoFijo() {
    String key = "b4a3a712-6f89-4d48-80c1-ec1d5b37cf22/dni_c4/3c52f7e4-cf2d-4a5e-b253-4818b3206a11.pdf";
    storage.guardar("matricula-documentos", key, new byte[] {1});
    String url = storage.urlFirmada("matricula-documentos", key);
    Map<String, String> query = parseUrl(url);
    assertTrue(url.startsWith("http://localhost:10000/api/v1/storage/local/download?"));
    assertEquals(64, query.get("sig").length());
  }

  @Test
  void normalizaUnaUrlConMuchasBarrasFinalesSinBacktracking() {
    var store = new LocalPrivateFileStore(
        tempDir.resolve("slashes"), " http://localhost:10000" + "/".repeat(100_000) + " ",
        60, SIGNING_KEY, Clock.fixed(NOW, ZoneOffset.UTC));
    store.guardar("matricula-documentos", "registro/documento.pdf", new byte[] {1});
    assertTrue(store.urlFirmada("matricula-documentos", "registro/documento.pdf")
        .startsWith("http://localhost:10000/api/v1/storage/local/download?"));
  }

  @Test
  void descargaTieneIgualdadPorContenidoYNoExponeBytesMutablesNiContenidoEnLogs() {
    byte[] original = "%PDF-documento-privado".getBytes(StandardCharsets.UTF_8);
    byte[] expected = original.clone();
    var archivo = new LocalPrivateFileStore.ArchivoDescargable(original, "application/pdf", "dni.pdf");
    var equivalente = new LocalPrivateFileStore.ArchivoDescargable(expected, "application/pdf", "dni.pdf");
    original[0] = 0;
    archivo.bytes()[1] = 0;
    assertArrayEquals(expected, archivo.bytes());
    assertEquals(equivalente, archivo);
    assertEquals(equivalente.hashCode(), archivo.hashCode());
    org.junit.jupiter.api.Assertions.assertNotEquals(
        new LocalPrivateFileStore.ArchivoDescargable(new byte[] {1}, "application/pdf", "dni.pdf"), archivo);
    org.junit.jupiter.api.Assertions.assertFalse(archivo.toString().contains("documento-privado"));
    assertTrue(archivo.toString().contains("size=" + expected.length));
  }

  private static byte[] descargar(LocalPrivateFileStore store, Map<String, String> query) {
    var archivo = store.descargar(
        query.get("bucket"), query.get("key"), Long.parseLong(query.get("expires")), query.get("sig"));
    assertEquals("application/pdf", archivo.mediaType());
    return archivo.bytes();
  }

  private static Map<String, String> parseUrl(String url) {
    String query = java.net.URI.create(url).getRawQuery();
    Map<String, String> values = new HashMap<>();
    for (String pair : query.split("&")) {
      String[] parts = pair.split("=", 2);
      values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
    }
    return values;
  }
}
