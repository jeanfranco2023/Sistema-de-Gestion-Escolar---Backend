package com.colegio.shuji.matricula.infrastructure.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService;
import com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService;
import com.colegio.shuji.shared.infrastructure.controller.exception.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class SolicitudMatriculaPublicaControllerTest {
  @Test
  void fichaSinDocumentosEmitidosDevuelve409JsonConAcceptDeNavegadorYEstrictHtml()
      throws Exception {
    var solicitudes = mock(SolicitudMatriculaPublicaService.class);
    when(solicitudes.generarFichaMatriculaHtml(any(UUID.class), anyString()))
        .thenThrow(new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Los documentos estarán disponibles al confirmar el pago y la matrícula"));

    var controller = new SolicitudMatriculaPublicaController(
        solicitudes,
        mock(GestionarPagoMatriculaPublicaUseCase.class),
        mock(GeminiDocumentoMatriculaPort.class),
        mock(FirmaDigitalDocumentoPort.class),
        mock(VerificacionIdentidadMatriculaService.class),
        mock(ReniecServicePort.class),
        mock(AlmacenDocumentosMatriculaPort.class),
        mock(com.colegio.shuji.shared.application.port.out.ActorActualPort.class));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    UUID solicitudId = UUID.randomUUID();

    mvc.perform(get("/api/v1/solicitudes-matricula/public/{id}/ficha", solicitudId)
            .header("X-Solicitud-Matricula-Token", "token-de-prueba")
            .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("HTTP_409"));

    mvc.perform(get("/api/v1/solicitudes-matricula/public/{id}/ficha", solicitudId)
            .header("X-Solicitud-Matricula-Token", "token-de-prueba")
            .header("Accept", "text/html"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("HTTP_409"));
  }
}
