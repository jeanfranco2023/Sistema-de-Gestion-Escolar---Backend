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
import com.colegio.shuji.matricula.application.dto.out.ApoderadoFichaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort.Identidad;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.infrastructure.controller.exception.MatriculaExceptionHandler;
import com.colegio.shuji.shared.infrastructure.controller.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
            7L, null, TipoDocumento.DNI, "12345678", "María", "Pérez", "López",
            LocalDate.of(2015, 5, 12), Genero.F, null, false, OrigenRegistro.MANUAL_CONTINGENCIA,
            null, null, true, OffsetDateTime.now()));

    mvc.perform(post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.codigoInterno").value("ALU-000007"))
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

  @Test
  void listarApoderadosPorEstudianteDevuelveLista() throws Exception {
    var apoderado = new ApoderadoResponseDto(
        10L, 20L, TipoDocumento.DNI, "87654321", "Carlos", "Pérez", "Gómez",
        "987654321", "carlos@gmail.com", "Av. Sol 123", "150101", true,
        OrigenRegistro.MANUAL_CONTINGENCIA, OffsetDateTime.now());
    var ficha = new ApoderadoFichaResponseDto(apoderado, Parentesco.PADRE, true);

    when(familias.listarApoderadosPorEstudiante(7L)).thenReturn(List.of(ficha));

    mvc.perform(get("/api/v1/estudiantes/7/apoderados"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].apoderado.numeroDocumento").value("87654321"))
        .andExpect(jsonPath("$[0].parentesco").value("PADRE"))
        .andExpect(jsonPath("$[0].esResponsableEconomico").value(true));
  }

  @Test
  void listarApoderadosPorEstudianteTienePreAuthorizeExclusivoDireccionYSecretaria() throws NoSuchMethodException {
    var metodo = EstudianteController.class.getDeclaredMethod("listarApoderadosPorEstudiante", Long.class);
    var preAuth = metodo.getAnnotation(PreAuthorize.class);
    org.junit.jupiter.api.Assertions.assertNotNull(preAuth, "El endpoint debe tener anotación @PreAuthorize");
    org.junit.jupiter.api.Assertions.assertEquals(
        "hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')",
        preAuth.value(),
        "El endpoint GET /api/v1/estudiantes/{id}/apoderados debe permitir exclusivamente a DIRECCION, ADMINISTRADOR y SECRETARIA");
  }

  private RegistrarEstudianteRequestDto solicitud(String documento, String nombres) {
    return new RegistrarEstudianteRequestDto(TipoDocumento.DNI, documento, nombres, "Pérez", "López",
        LocalDate.of(2015, 5, 12), Genero.F, null, null);
  }
}
