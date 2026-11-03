package com.colegio.shuji.matricula.infrastructure.controller.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort.Identidad;
import com.colegio.shuji.shared.infrastructure.controller.exception.GlobalExceptionHandler;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReniecControllerTest {

  private ReniecServicePort reniecServicePort;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    reniecServicePort = mock(ReniecServicePort.class);
    var controller = new ReniecController(reniecServicePort);
    mvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("Consulta exitosa devuelve HTTP 200 y datos de la persona")
  void consultaExitosaDevuelveDatosPersona() throws Exception {
    String dni = "12345678";
    when(reniecServicePort.consultar(dni))
        .thenReturn(Optional.of(new Identidad(dni, "JUAN CARLOS", "PEREZ", "GARCIA")));

    mvc.perform(get("/api/v1/personas/reniec/{dni}", dni))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numeroDocumento").value(dni))
        .andExpect(jsonPath("$.nombres").value("JUAN CARLOS"))
        .andExpect(jsonPath("$.apellidoPaterno").value("PEREZ"))
        .andExpect(jsonPath("$.apellidoMaterno").value("GARCIA"));
  }

  @Test
  @DisplayName("DNI no encontrado en proveedor devuelve HTTP 404")
  void dniNoEncontradoDevuelve404() throws Exception {
    String dni = "87654321";
    when(reniecServicePort.consultar(dni)).thenReturn(Optional.empty());

    mvc.perform(get("/api/v1/personas/reniec/{dni}", dni))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.errorCode").value("HTTP_404"));
  }

  @Test
  @DisplayName("DNI con longitud menor a 8 digitos devuelve HTTP 400 Bad Request sin invocar servicio")
  void dniCortoDevuelve400() throws Exception {
    mvc.perform(get("/api/v1/personas/reniec/{dni}", "12345"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("HTTP_400"));

    verifyNoInteractions(reniecServicePort);
  }

  @Test
  @DisplayName("DNI con caracteres no numéricos devuelve HTTP 400 Bad Request")
  void dniNoNumericoDevuelve400() throws Exception {
    mvc.perform(get("/api/v1/personas/reniec/{dni}", "1234567A"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(reniecServicePort);
  }

  @Test
  @DisplayName("DNI con longitud mayor a 8 dígitos devuelve HTTP 400 Bad Request")
  void dniLargoDevuelve400() throws Exception {
    mvc.perform(get("/api/v1/personas/reniec/{dni}", "123456789"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(reniecServicePort);
  }
}
