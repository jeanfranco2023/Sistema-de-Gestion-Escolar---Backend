package com.colegio.shuji.shared.infrastructure.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {
  @org.mockito.Mock private AnioLectivoRepositoryPort anios;
  @org.mockito.Mock private MatriculaRepositoryPort matriculas;
  @org.mockito.Mock private AsistenciaAulaRepositoryPort asistencias;
  @org.mockito.Mock private CalificacionRepositoryPort calificaciones;
  @org.mockito.Mock private PagoRepositoryPort pagos;
  private MockMvc mvc;

  @org.junit.jupiter.api.BeforeEach
  void configurar() {
    org.mockito.MockitoAnnotations.openMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(new DashboardController(anios, matriculas, asistencias,
        calificaciones, pagos)).build();
  }

  @Test
  void retornaResumen200ConMetricasSegurasSiFallanLasFuentes() throws Exception {
    when(anios.listar()).thenThrow(new IllegalStateException("fuente temporalmente indisponible"));

    mvc.perform(get("/api/v1/dashboard/resumen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anio").isNumber())
        .andExpect(jsonPath("$.totalEstudiantes").value(0))
        .andExpect(jsonPath("$.asistenciaHoy").value(0))
        .andExpect(jsonPath("$.recaudacionMes").value(0))
        .andExpect(jsonPath("$.alumnosEnRiesgo").value(0));
  }

  @Test
  void calculaMatriculadosYNoDividePorCero() throws Exception {
    var actual = (short) java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).getYear();
    when(anios.listar()).thenReturn(List.of(AnioLectivo.builder().id((short) 1).anio(actual).build()));
    when(matriculas.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
    when(calificaciones.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
    when(asistencias.buscarPorFechaSesion(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"))))
        .thenReturn(List.of());
    when(pagos.listar()).thenReturn(List.of());

    mvc.perform(get("/api/v1/dashboard/resumen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anio").value((int) actual))
        .andExpect(jsonPath("$.asistenciaHoy").value(0))
        .andExpect(jsonPath("$.recaudacionMes").value(0));
  }
}
