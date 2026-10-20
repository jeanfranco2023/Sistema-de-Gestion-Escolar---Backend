package com.colegio.shuji.shared.infrastructure.controller.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void conservaElEstadoHttpDeResponseStatusException() {
    var request = request("GET", "/api/v1/solicitudes-matricula/public/dni/73602651");
    var response = handler.handleResponseStatusException(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor sin resultado"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("HTTP_404", response.getBody().getErrorCode());
    assertEquals("Proveedor sin resultado", response.getBody().getMessage());
    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
  }

  @Test
  void convierteEnumInvalidoEn400ConCampoYValoresAceptados() throws Exception {
    JsonProcessingException cause = org.junit.jupiter.api.Assertions.assertThrows(
        JsonProcessingException.class,
        () -> new ObjectMapper().readValue("{\"estado\":\"DESCONOCIDO\"}", EnumPayload.class));

    var response = handler.handleHttpMessageNotReadable(
        new HttpMessageNotReadableException("detalle interno", cause),
        request("POST", "/api/v1/ejemplo"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().getMessage().contains("estado"));
    assertTrue(response.getBody().getMessage().contains("ACTIVO, INACTIVO"));
    assertFalse(response.getBody().getMessage().contains("DESCONOCIDO"));
    assertFalse(response.getBody().getMessage().contains("detalle interno"));
    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
  }

  @Test
  void convierteTipoDeParametroIncorrectoEn400() {
    var response = handler.handleMethodArgumentTypeMismatch(
        new MethodArgumentTypeMismatchException("abc", Long.class, "estudianteId", null, null),
        request("GET", "/api/v1/estudiantes"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().getMessage().contains("estudianteId"));
  }

  @Test
  void convierteParametroRequeridoAusenteEn400() {
    var response = handler.handleMissingServletRequestParameter(
        new MissingServletRequestParameterException("anioId", "Long"),
        request("GET", "/api/v1/academico/periodos"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().getMessage().contains("anioId"));
  }

  @Test
  void convierteMetodoNoPermitidoEn405() {
    var response = handler.handleHttpRequestMethodNotSupported(
        new HttpRequestMethodNotSupportedException("TRACE"),
        request("GET", "/api/v1/estudiantes"));

    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }

  @Test
  void convierteFormatoDeRespuestaNoAceptableEn406() {
    var response = handler.handleHttpMediaTypeNotAcceptable(
        new HttpMediaTypeNotAcceptableException("sin representación"),
        request("GET", "/api/v1/documentos"));

    assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
  }

  @Test
  void convierteTipoDeContenidoNoSoportadoEn415() {
    var response = handler.handleHttpMediaTypeNotSupported(
        new HttpMediaTypeNotSupportedException("tipo no soportado"),
        request("POST", "/api/v1/estudiantes"));

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
  }

  @Test
  void convierteRecursoEstaticoAusenteEn404() {
    var response = handler.handleNoResourceFound(
        new NoResourceFoundException(HttpMethod.GET, "missing.css"),
        request("GET", "/missing.css"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
  }

  private MockHttpServletRequest request(String method, String path) {
    return new MockHttpServletRequest(method, path);
  }

  private enum EstadoTest {
    ACTIVO,
    INACTIVO
  }

  private record EnumPayload(EstadoTest estado) {}
}