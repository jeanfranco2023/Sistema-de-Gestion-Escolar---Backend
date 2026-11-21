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
  void retornaResumen200ConMetricasNulasSiFallanLasFuentes() throws Exception {
    when(anios.listar()).thenThrow(new IllegalStateException("fuente temporalmente indisponible"));

    mvc.perform(get("/api/v1/dashboard/resumen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anio").isNumber())
        .andExpect(jsonPath("$.totalEstudiantes").doesNotExist())
        .andExpect(jsonPath("$.alumnosEnRiesgo").doesNotExist());
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

  @Test
  void retornaIndicadoresDireccionConDesempenoCoberturaYAsistencia() throws Exception {
    var actual = (short) java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).getYear();
    when(anios.listar()).thenReturn(List.of(AnioLectivo.builder().id((short) 1).anio(actual).build()));
    when(matriculas.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(
        com.colegio.shuji.matricula.domain.model.Matricula.builder()
            .id(101L)
            .anioLectivoId((short) 1)
            .estudianteId(10L)
            .seccionId(1)
            .estadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO)
            .build(),
        com.colegio.shuji.matricula.domain.model.Matricula.builder()
            .id(102L)
            .anioLectivoId((short) 1)
            .estudianteId(11L)
            .seccionId(1)
            .estadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO)
            .build()));
    when(calificaciones.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(
        com.colegio.shuji.evaluacion.domain.model.CalificacionCneb.builder()
            .id(1L)
            .matriculaId(101L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 1)
            .calificacionCualitativa(com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa.AD)
            .build()));
    when(asistencias.buscarPorMatriculaIds(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(
        com.colegio.shuji.asistencia.domain.model.AsistenciaAula.builder()
            .id(1L)
            .matriculaId(101L)
            .fechaSesion(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")))
            .estado(com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula.PRESENTE)
            .build()));

    mvc.perform(get("/api/v1/dashboard/indicadores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desempeno.destacados").value(1))
        .andExpect(jsonPath("$.desempeno.totalEvaluados").value(1))
        .andExpect(jsonPath("$.desempeno.sinEvaluacion").value(1))
        .andExpect(jsonPath("$.cobertura.porcentajeCobertura").value(50.0))
        .andExpect(jsonPath("$.asistenciaCupos.porcentajeAsistencia").value(100.0));
  }
}
