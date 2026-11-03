package com.colegio.shuji.usuario.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PdfBoxGeminiValidadorDocumentoEmpleadoAdapterTest {

  private static final String ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";

  @Test
  void falloDeTransporteGeminiQuedaObservadoYNoUsaHeuristica() throws IOException {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    var adapter = new PdfBoxGeminiValidadorDocumentoEmpleadoAdapter(
        new ObjectMapper(), "test-key", "", "test-model", builder.build());

    var result = adapter.validar(TipoDocumentoEmpleado.CV, pdfValido(), usuarioConDni());

    assertEquals(EstadoValidacionIaEmpleado.OBSERVADO_IA, result.estado());
    assertEquals("No disponible", result.motorValidacion());
    server.verify();
  }

  @Test
  void respuestaGeminiSinTodosLosBooleanosQuedaObservada() throws IOException {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    String response = """
        {"candidates":[{"content":{"parts":[{"text":"{\\"tipoCorrecto\\":true,\\"legible\\":true,\\"observacion\\":\\"incompleta\\"}"}]}}]}
        """;
    server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    var adapter = new PdfBoxGeminiValidadorDocumentoEmpleadoAdapter(
        new ObjectMapper(), "test-key", "", "test-model", builder.build());

    var result = adapter.validar(TipoDocumentoEmpleado.CV, pdfValido(), usuarioConDni());

    assertEquals(EstadoValidacionIaEmpleado.OBSERVADO_IA, result.estado());
    server.verify();
  }

  private static Usuario usuarioConDni() {
    return Usuario.builder().id(5L).username("empleado_test").dni("12345678").build();
  }

  private static byte[] pdfValido() throws IOException {
    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage());
      var output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }
}
