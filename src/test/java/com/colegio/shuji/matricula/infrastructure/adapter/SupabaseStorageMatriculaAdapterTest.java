package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SupabaseStorageMatriculaAdapterTest {
  @Test
  void subeArchivoAlBucketPrivadoConRutaNoEnumerada() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var path = new AtomicReference<String>();
    var authorization = new AtomicReference<String>();
    var content = new AtomicReference<byte[]>();
    server.createContext("/storage/v1/bucket/matricula-documentos", exchange -> {
      byte[] data = "{\"id\":\"matricula-documentos\",\"public\":false}".getBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, data.length);
      try (var output = exchange.getResponseBody()) { output.write(data); }
    });
    server.createContext("/storage/v1/object/matricula-documentos/", exchange -> {
      path.set(exchange.getRequestURI().getPath());
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      content.set(exchange.getRequestBody().readAllBytes());
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    try {
      var id = UUID.fromString("b4ccf59e-44dc-46dc-8cbe-c658008041dd");
      var adapter = new SupabaseStorageMatriculaAdapter(
          "http://127.0.0.1:" + server.getAddress().getPort(), "server-only-test-key", "matricula-documentos", 60);
      var bytes = new byte[] {1, 2, 3, 4};
      var result = adapter.guardar(id, TipoDocumentoSolicitud.DNI_C4, bytes, "image/png");
      assertEquals("matricula-documentos", result.bucket());
      assertEquals("/storage/v1/object/matricula-documentos/" + id + "/dni_c4.png", path.get());
      assertEquals("Bearer server-only-test-key", authorization.get());
      assertArrayEquals(bytes, content.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void fallaDeFormaExplicitaSiLaStorageNoEstaConfigurada() {
    var adapter = new SupabaseStorageMatriculaAdapter("", "", "matricula-documentos", 60);
    assertThrows(ResponseStatusException.class, adapter::validarConfiguracion);
  }

  @Test
  void creaElBucketPrivadoConLimitesSiAunNoExiste() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var bucketCreated = new AtomicBoolean();
    server.createContext("/storage/v1/bucket/matricula-documentos", exchange -> {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
    });
    server.createContext("/storage/v1/bucket", exchange -> {
      String request = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      bucketCreated.set(request.contains("\"public\":false")
          && request.contains("\"fileSizeLimit\":5242880"));
      byte[] data = "{\"id\":\"matricula-documentos\",\"public\":false}".getBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, data.length);
      try (var output = exchange.getResponseBody()) { output.write(data); }
    });
    server.createContext("/storage/v1/object/matricula-documentos/", exchange -> {
      exchange.getRequestBody().readAllBytes();
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    try {
      var adapter = new SupabaseStorageMatriculaAdapter(
          "http://127.0.0.1:" + server.getAddress().getPort(), "server-only-test-key", "matricula-documentos", 60);
      adapter.guardar(UUID.randomUUID(), TipoDocumentoSolicitud.DNI_C4, new byte[] {1}, "image/png");
      org.junit.jupiter.api.Assertions.assertTrue(bucketCreated.get());
    } finally {
      server.stop(0);
    }
  }
}
