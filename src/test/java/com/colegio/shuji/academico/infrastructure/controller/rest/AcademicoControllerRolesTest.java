package com.colegio.shuji.academico.infrastructure.controller.rest;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.academico.application.port.in.ConsultarVacantesUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAulasUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarSeccionesUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AcademicoControllerRolesTest.MethodSecurityTestConfig.class)
class AcademicoControllerRolesTest {
  @Autowired private ApplicationContext context;
  @Autowired private GestionarAnioLectivoUseCase calendario;
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    reset(calendario);
    when(calendario.listarPeriodos((short) 2026)).thenReturn(List.of());
    mvc = MockMvcBuilders.standaloneSetup(context.getBean(AcademicoController.class))
        .addFilters(new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint()))
        .build();
  }

  @Test
  @WithMockUser(roles = "DOCENTE")
  void docentePuedeListarPeriodos() throws Exception {
    mvc.perform(get("/api/v1/academico/periodos").param("anioId", "2026"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "APODERADO")
  void apoderadoNoPuedeListarPeriodos() throws Exception {
    mvc.perform(get("/api/v1/academico/periodos").param("anioId", "2026"))
        .andExpect(status().isForbidden());
  }

  @Configuration
  @EnableMethodSecurity
  static class MethodSecurityTestConfig {
    @Bean GestionarAnioLectivoUseCase calendario() { return org.mockito.Mockito.mock(GestionarAnioLectivoUseCase.class); }
    @Bean GestionarSeccionesUseCase estructura() { return org.mockito.Mockito.mock(GestionarSeccionesUseCase.class); }
    @Bean ConsultarVacantesUseCase cupos() { return org.mockito.Mockito.mock(ConsultarVacantesUseCase.class); }
    @Bean GestionarAulasUseCase aulas() { return org.mockito.Mockito.mock(GestionarAulasUseCase.class); }
    @Bean AcademicoController academicoController(GestionarAnioLectivoUseCase calendario,
        GestionarSeccionesUseCase estructura, ConsultarVacantesUseCase cupos,
        GestionarAulasUseCase aulas) {
      return new AcademicoController(calendario, estructura, cupos, aulas);
    }
  }
}
