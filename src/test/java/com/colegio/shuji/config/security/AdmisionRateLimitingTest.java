package com.colegio.shuji.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdmisionRateLimitingTest {
  private final RateLimitingFilter filter = new RateLimitingFilter();
  private final FilterChain continuar = (request, response) -> {};
  private static final String PUBLICO = "/api/v1/solicitudes-matricula/public";

  @Test
  void crearComparteLimiteConYSinBarraYNoBloqueaCatalogoNiPreflight() throws Exception {
    for (int i = 0; i < 5; i++) assertEquals(200, enviar("POST", PUBLICO, "10.0.0.1").getStatus());
    var rechazo = enviar("POST", PUBLICO + "/", "10.0.0.1");
    assertEquals(429, rechazo.getStatus());
    assertEquals("60", rechazo.getHeader("Retry-After"));
    assertEquals(200, enviar("GET", PUBLICO + "/catalogo", "10.0.0.1").getStatus());
    assertEquals(200, enviar("OPTIONS", PUBLICO, "10.0.0.1").getStatus());
    assertEquals(200, enviar("POST", PUBLICO, "10.0.0.2").getStatus());
  }

  @Test
  void documentosCompartenLimiteAunqueCambieExpedienteOTipo() throws Exception {
    for (int i = 0; i < 10; i++) {
      assertEquals(200, enviar("POST", PUBLICO + "/exp-" + i + "/documentos/DNI_C4", "10.0.0.1").getStatus());
    }
    assertEquals(429, enviar("POST", PUBLICO + "/otro/documentos/PARTIDA_NACIMIENTO/", "10.0.0.1").getStatus());
    assertEquals(200, enviar("GET", PUBLICO + "/otro/documentos/DNI_C4", "10.0.0.1").getStatus());
    assertEquals(200, enviar("POST", PUBLICO, "10.0.0.1").getStatus());
  }

  @Test
  void consultasDniCompartenContadorParaEvitarRotacionDelNumero() throws Exception {
    for (int i = 0; i < 8; i++) {
      assertEquals(200, enviar("GET", PUBLICO + "/dni/1234567" + i, "10.0.0.1").getStatus());
    }
    assertEquals(429, enviar("GET", PUBLICO + "/dni/99999999", "10.0.0.1").getStatus());
  }

  private MockHttpServletResponse enviar(String metodo, String uri, String ip) throws Exception {
    var request = new MockHttpServletRequest(metodo, uri);
    request.setRemoteAddr(ip);
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, continuar);
    return response;
  }

  @Test
  void limitaLasSugerenciasDeIaParaProtegerElProveedor() throws Exception {
    String ruta="/api/v1/evaluacion/ia/sugerir-conclusion";
    for(int i=0;i<15;i++) assertEquals(200,enviar("POST",ruta,"10.0.0.1").getStatus());
    assertEquals(429,enviar("POST",ruta,"10.0.0.1").getStatus());
  }
}
