package com.colegio.shuji.evaluacion.infrastructure.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase;
import com.colegio.shuji.evaluacion.application.port.in.SugerirConclusionDescriptivaUseCase;
import com.colegio.shuji.evaluacion.application.port.in.ImportarCalificacionesUseCase;
import com.colegio.shuji.evaluacion.application.port.in.GenerarLibretaPdfUseCase;
import com.colegio.shuji.evaluacion.application.port.in.DescargarPlantillaCnebUseCase;
import com.colegio.shuji.evaluacion.application.dto.out.ImportacionCalificacionesResponseDto;
import com.colegio.shuji.evaluacion.infrastructure.controller.rest.EvaluacionController;
import com.colegio.shuji.shared.infrastructure.controller.exception.GlobalExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.web.MockMultipartFile;

@SpringJUnitConfig(EvaluacionPlaybookControllerTest.Config.class)
@WebAppConfiguration
class EvaluacionPlaybookControllerTest {
  @Autowired WebApplicationContext contexto; @Autowired ImportarCalificacionesUseCase importacion;
  @Autowired GenerarLibretaPdfUseCase pdf; @Autowired SugerirConclusionDescriptivaUseCase ia;
  MockMvc mvc;
  @BeforeEach void preparar() { reset(importacion,pdf,ia);mvc=MockMvcBuilders.webAppContextSetup(contexto).build(); }
  private MockMultipartFile archivo(String contenido) { return new MockMultipartFile("archivo","notas.csv","text/csv",contenido.getBytes(StandardCharsets.UTF_8)); }
  @Test @WithMockUser(roles="DOCENTE")
  void multipartPasaPorParserRealYConservaContextoDeConfirmacion() throws Exception {
    when(importacion.importar(anyList(),eq((short)1),eq(2),eq(9L),eq(false))).thenReturn(new ImportacionCalificacionesResponseDto(1,0,List.of()));
    mvc.perform(multipart("/api/v1/evaluacion/cneb/carga-masiva").file(archivo("DNI_ESTUDIANTE,COMPETENCIA_ID,NOTA_CNEB,CONCLUSION_DESCRIPTIVA\n00123456,8,B,Explica con apoyo"))
        .param("periodoId","1").param("seccionId","2").param("asignacionDocenteId","9"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.filasProcesadas").value(0));
    verify(importacion).importar(argThat(f->f.get(1).getFirst().equals("00123456")),eq((short)1),eq(2),eq(9L),eq(false));
  }
  @Test @WithMockUser(roles="DOCENTE")
  void csvCorruptoNoInvocaPersistencia() throws Exception {
    mvc.perform(multipart("/api/v1/evaluacion/cneb/carga-masiva").file(archivo("a,b,c,\"truncado"))
        .param("periodoId","1").param("seccionId","2").param("asignacionDocenteId","9"))
        .andExpect(status().isBadRequest()); verifyNoInteractions(importacion);
  }
  @Test @WithMockUser(roles="APODERADO")
  void apoderadoNoPuedeImportarNiConsumirLaIaDocente() throws Exception {
    mvc.perform(multipart("/api/v1/evaluacion/cneb/carga-masiva").file(archivo("a,b,c,d"))
        .param("periodoId","1").param("seccionId","2").param("asignacionDocenteId","9")).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/evaluacion/ia/sugerir-conclusion").contentType("application/json")
        .content("{\"areaCurricular\":\"Matemática\",\"competenciaDescripcion\":\"Resuelve problemas\",\"nivelLogro\":\"B\",\"desempenoObservado\":\"Resuelve con apoyo\"}"))
        .andExpect(status().isForbidden()); verifyNoInteractions(importacion,ia);
  }
  @Test @WithMockUser(roles="APODERADO")
  void descargaPdfUsaElCasoDeUsoQueValidaCustodia() throws Exception {
    when(pdf.generar(7L)).thenReturn(com.colegio.shuji.evaluacion.infrastructure.adapter.ReporteLibretaPdfAdapter.renderizar(List.of("Informe de prueba")));
    mvc.perform(get("/api/v1/evaluacion/libreta/7/pdf")).andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf")).andExpect(header().string("Content-Disposition","attachment; filename=\"libreta_7.pdf\""));
    verify(pdf).generar(7L);
  }
  @Configuration @EnableWebMvc @EnableMethodSecurity
  static class Config {
    @Bean RegistrarEvaluacionCnebUseCase registro(){return mock(RegistrarEvaluacionCnebUseCase.class);}
    @Bean ConsultarLibretaNotasUseCase consulta(){return mock(ConsultarLibretaNotasUseCase.class);}
    @Bean SugerirConclusionDescriptivaUseCase ia(){return mock(SugerirConclusionDescriptivaUseCase.class);}
    @Bean ImportarCalificacionesUseCase importacion(){return mock(ImportarCalificacionesUseCase.class);}
    @Bean GenerarLibretaPdfUseCase pdf(){return mock(GenerarLibretaPdfUseCase.class);}
    @Bean DescargarPlantillaCnebUseCase plantilla(){return mock(DescargarPlantillaCnebUseCase.class);}
    @Bean GlobalExceptionHandler errores(){return new GlobalExceptionHandler();}
    @Bean EvaluacionController controlador(RegistrarEvaluacionCnebUseCase r,ConsultarLibretaNotasUseCase c,SugerirConclusionDescriptivaUseCase ia,ImportarCalificacionesUseCase i,GenerarLibretaPdfUseCase p,DescargarPlantillaCnebUseCase plantilla){return new EvaluacionController(r,c,ia,i,p,plantilla);}
  }
}
