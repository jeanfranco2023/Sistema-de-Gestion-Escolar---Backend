package com.colegio.shuji.evaluacion.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.colegio.shuji.evaluacion.application.dto.in.SugerirConclusionDescriptivaRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GeminiConclusionDescriptivaTest {
  private final ObjectMapper json = new ObjectMapper();
  private final SugerirConclusionDescriptivaRequestDto request = new SugerirConclusionDescriptivaRequestDto(
      "Ana", "Matemática", "Resuelve problemas", "B", "Necesita apoyo para verificar resultados");
  @Test void usaHttpRealEstructuradoSinEnviarIdentidadYExigeRespuestaCompleta() throws Exception {
    var builder = RestClient.builder(); var servidor = MockRestServiceServer.bindTo(builder).build();
    var adapter = new GeminiConclusionDescriptivaAdapter("clave-sintetica", "gemini-2.5-flash", json, builder.build());
    String resultado = json.writeValueAsString(java.util.Map.of("conclusionDescriptiva", "Identifica datos y requiere apoyo para comprobar resultados.",
        "aspectoMejora", "Comprobar resultados", "recomendacionApoderado", "Practicar con ejemplos cotidianos"));
    String body = json.writeValueAsString(java.util.Map.of("candidates", java.util.List.of(java.util.Map.of("content", java.util.Map.of("parts", java.util.List.of(java.util.Map.of("text", resultado)))))));
    servidor.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
        .andExpect(header("x-goog-api-key", "clave-sintetica"))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Ana"))))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    var respuesta = adapter.generar(request); assertTrue(respuesta.generadoPorIa());
    assertTrue(respuesta.conclusionDescriptiva().contains("Identifica")); servidor.verify();
  }
  @Test void sinClaveOConRespuestaIncompletaNoInventaUnaSugerencia() {
    var adapter = new GeminiConclusionDescriptivaAdapter("", "modelo", json, RestClient.create());
    assertEquals(503, assertThrows(ResponseStatusException.class, () -> adapter.generar(request)).getStatusCode().value());
    var builder = RestClient.builder(); var servidor = MockRestServiceServer.bindTo(builder).build();
    var configurado = new GeminiConclusionDescriptivaAdapter("sintetica", "modelo", json, builder.build());
    servidor.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    assertEquals(503, assertThrows(ResponseStatusException.class, () -> configurado.generar(request)).getStatusCode().value());
  }
}
