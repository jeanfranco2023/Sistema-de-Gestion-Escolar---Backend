package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReniecHttpAdapterTest {
  @Test
  void mapeaRespuestaDeProveedorExterno() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/dni", exchange -> {
      assertEquals("numero=12345678", exchange.getRequestURI().getRawQuery());
      assertEquals("Bearer token-test", exchange.getRequestHeaders().getFirst("Authorization"));
      var json = "{\"first_name\":\"Ana\",\"first_last_name\":\"Pérez\","
          + "\"second_last_name\":\"López\",\"document_number\":\"12345678\"}";
      byte[] data = json.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(200, data.length);
      try (var output = exchange.getResponseBody()) { output.write(data); }
    });
    server.start();
    try {
      var adapter = new ReniecHttpAdapter(
          "http://127.0.0.1:" + server.getAddress().getPort() + "/dni?numero={dni}",
          "token-test", "");
      var identidad = adapter.consultar("12345678");
      assertTrue(identidad.isPresent());
      assertEquals("Ana", identidad.get().nombres());
      assertEquals("Pérez", identidad.get().apellidoPaterno());
      assertEquals("López", identidad.get().apellidoMaterno());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void noAceptaDocumentoInvalidoNiProveedorAusente() {
    var adapter = new ReniecHttpAdapter("", "", "");
    assertFalse(adapter.consultar("12345678").isPresent());
    assertFalse(adapter.consultar("123").isPresent());
  }

  @Test
  void apiPeruUsaPostYApiKeyComoBearer() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/dni", exchange -> {
      assertEquals("POST", exchange.getRequestMethod());
      assertEquals("Bearer key-test", exchange.getRequestHeaders().getFirst("Authorization"));
      String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(request.contains("\"dni\":\"12345678\""));
      var json = "{\"success\":true,\"data\":{\"numero\":\"12345678\","
          + "\"nombres\":\"Ana\",\"apellido_paterno\":\"Pérez\","
          + "\"apellido_materno\":\"López\"}}";
      byte[] data = json.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(200, data.length);
      try (var output = exchange.getResponseBody()) { output.write(data); }
    });
    server.start();
    try {
      var adapter = new ReniecHttpAdapter(
          "http://127.0.0.1:" + server.getAddress().getPort() + "/dni", "token-viejo", "key-test");
      var identidad = adapter.consultar("12345678");
      assertTrue(identidad.isPresent());
      assertEquals("Ana", identidad.get().nombres());
      assertEquals("Pérez", identidad.get().apellidoPaterno());
      assertEquals("López", identidad.get().apellidoMaterno());
    } finally {
      server.stop(0);
    }
  }
}
