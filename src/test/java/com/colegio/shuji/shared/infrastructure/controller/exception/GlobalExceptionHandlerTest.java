package com.colegio.shuji.shared.infrastructure.controller.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {
  @Test
  void conservaElEstadoHttpDeResponseStatusException() {
    var request = new MockHttpServletRequest("GET", "/api/v1/solicitudes-matricula/public/dni/73602651");
    var response = new GlobalExceptionHandler().handleResponseStatusException(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor sin resultado"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("HTTP_404", response.getBody().getErrorCode());
    assertEquals("Proveedor sin resultado", response.getBody().getMessage());
  }
}
