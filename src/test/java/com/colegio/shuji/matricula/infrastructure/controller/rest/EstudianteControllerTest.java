package com.colegio.shuji.matricula.infrastructure.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort.Identidad;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.infrastructure.controller.exception.MatriculaExceptionHandler;
import com.colegio.shuji.shared.infrastructure.controller.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

class EstudianteControllerTest {
  @org.mockito.Mock private RegistrarFichaFamiliarUseCase familias;
  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @org.junit.jupiter.api.BeforeEach
  void configurar() {
    org.mockito.MockitoAnnotations.openMocks(this);
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mvc = MockMvcBuilders.standaloneSetup(new EstudianteController(familias))
        .setControllerAdvice(new MatriculaExceptionHandler(), new GlobalExceptionHandler())
        .build();
  }

  @Test
  @WithMockUser(roles = "SECRETARIA")
  void registraEstudianteConDtoValido() throws Exception {
    var request = solicitud("12345678", "María");
    when(familias.registrarEstudiante(any(RegistrarEstudianteRequestDto.class)))
        .thenReturn(new EstudianteResponseDto(
            7L, UUID.randomUUID(), TipoDocumento.DNI, "12345678", "María", "Pérez", "López",
            LocalDate.of(2015, 5, 12), Genero.F, null, false, OrigenRegistro.MANUAL_CONTINGENCIA,
            null, null, true, OffsetDateTime.now()));

    mvc.perform(post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.tipoDocumento").value("DNI"));
  }

  @Test
  void consultaDniDevuelveNombresYApellidos() throws Exception {
    when(familias.consultarDni("12345678"))
        .thenReturn(Optional.of(new Identidad("12345678", "Ana", "Pérez", "López")));
    mvc.perform(get("/api/v1/estudiantes/dni/12345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombres").value("Ana"))
        .andExpect(jsonPath("$.apellidoPaterno").value("Pérez"))
        .andExpect(jsonPath("$.apellidoMaterno").value("López"));
  }

  @Test
  @WithMockUser(roles = "SECRETARIA")
  void rechazaDocumentoDuplicado() throws Exception {
    when(familias.registrarEstudiante(any(RegistrarEstudianteRequestDto.class)))
        .thenThrow(new com.colegio.shuji.shared.domain.exception.BusinessException("Documento ya registrado"));

    mvc.perform(post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(solicitud("12345678", "María"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "SECRETARIA")
  void rechazaCamposVaciosSinInvocarCasoDeUso() throws Exception {
    mvc.perform(post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"12345678\",\"nombres\":\" \",\"apellidoPaterno\":\"Pérez\",\"apellidoMaterno\":\"López\",\"fechaNacimiento\":\"2015-05-12\",\"genero\":\"F\"}"))
        .andExpect(status().isBadRequest());

    verify(familias, never()).registrarEstudiante(any(RegistrarEstudianteRequestDto.class));
  }

  private RegistrarEstudianteRequestDto solicitud(String documento, String nombres) {
    return new RegistrarEstudianteRequestDto(TipoDocumento.DNI, documento, nombres, "Pérez", "López",
        LocalDate.of(2015, 5, 12), Genero.F, null, null, null);
  }
}
