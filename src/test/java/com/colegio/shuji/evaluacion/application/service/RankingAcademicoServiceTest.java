package com.colegio.shuji.evaluacion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.convivencia.application.port.out.IncidenciaRepositoryPort;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RankingAcademicoServiceTest {
  private final MatriculaRepositoryPort matriculas = mock(MatriculaRepositoryPort.class);
  private final EstudianteRepositoryPort estudiantes = mock(EstudianteRepositoryPort.class);
  private final SeccionRepositoryPort secciones = mock(SeccionRepositoryPort.class);
  private final GradoRepositoryPort grados = mock(GradoRepositoryPort.class);
  private final NivelRepositoryPort niveles = mock(NivelRepositoryPort.class);
  private final CalificacionRepositoryPort calificaciones = mock(CalificacionRepositoryPort.class);
  private final AsistenciaAulaRepositoryPort asistencias = mock(AsistenciaAulaRepositoryPort.class);
  private final IncidenciaRepositoryPort incidencias = mock(IncidenciaRepositoryPort.class);
  private final AsignacionRepositoryPort asignaciones = mock(AsignacionRepositoryPort.class);
  private final ActorActualPort actor = mock(ActorActualPort.class);
  private final RankingAcademicoService service = new RankingAcademicoService(
      matriculas, estudiantes, secciones, grados, niveles, calificaciones,
      asistencias, incidencias, asignaciones, actor);

  private final Seccion seccion = Seccion.builder()
      .id(101).anioLectivoId((short) 2026).gradoId((short) 3).nivelId((short) 1)
      .letra("A").build();
  private final Grado grado = Grado.builder().id((short) 3).nivelId((short) 1).nombre("Primero").build();

  @BeforeEach
  void prepararAccesoDeDocente() {
    when(actor.tieneRol("DIRECCION")).thenReturn(false);
    when(actor.tieneRol("SECRETARIA")).thenReturn(false);
    when(actor.tieneRol("DOCENTE")).thenReturn(true);
    when(actor.usuarioId()).thenReturn(70L);
    when(asignaciones.buscarPorDocenteUsuarioId(70L)).thenReturn(List.of(
        AsignacionDocente.builder().docenteUsuarioId(70L).seccionId(101).build()));
    when(grados.buscarPorId((short) 3)).thenReturn(Optional.of(grado));
    when(niveles.buscarPorId((short) 1)).thenReturn(Optional.of(
        Nivel.builder().id((short) 1).nombre("Primaria").build()));
  }

  @Test
  void rankingSoloIncluyeMatriculadosConNotasYConsultaDatosEnTresLotes() {
    Matricula matriculadoConNotas = Matricula.builder()
        .id(1L).estudianteId(201L).seccionId(101).estadoMatricula(EstadoMatricula.MATRICULADO).build();
    Matricula solicitadaConNotas = Matricula.builder()
        .id(2L).estudianteId(202L).seccionId(101).estadoMatricula(EstadoMatricula.SOLICITADA).build();
    Matricula matriculadoSinNotas = Matricula.builder()
        .id(3L).estudianteId(203L).seccionId(101).estadoMatricula(EstadoMatricula.MATRICULADO).build();
    when(secciones.buscarPorId(101)).thenReturn(Optional.of(seccion));
    when(matriculas.buscarPorSeccionId(101))
        .thenReturn(List.of(matriculadoConNotas, solicitadaConNotas, matriculadoSinNotas));
    when(estudiantes.buscarPorIds(List.of(201L, 203L))).thenReturn(List.of(
        Estudiante.builder().id(201L).tipoDocumento(com.colegio.shuji.matricula.domain.enums.TipoDocumento.DNI)
            .numeroDocumento("70000001").nombres("Ana").apellidoPaterno("Zárate").build(),
        Estudiante.builder().id(203L).nombres("Sin notas").apellidoPaterno("Prueba").build()));
    when(calificaciones.buscarPorMatriculaIds(List.of(1L, 3L))).thenReturn(List.of(
        CalificacionCneb.builder().matriculaId(1L).calificacionCualitativa(CalificacionCualitativa.AD).build(),
        CalificacionCneb.builder().matriculaId(1L).calificacionCualitativa(CalificacionCualitativa.A).build()));
    when(asistencias.buscarPorMatriculaIds(List.of(1L, 3L))).thenReturn(List.of());
    when(incidencias.buscarPorMatriculaIds(List.of(1L, 3L))).thenReturn(List.of());

    var cuadro = service.obtenerRankingSeccion(101);

    assertEquals(1, cuadro.estudiantes().size());
    assertEquals(201L, cuadro.estudiantes().getFirst().estudianteId());
    assertEquals(3.5, cuadro.estudiantes().getFirst().puntajePromedioInterno());
    assertEquals(0.0, cuadro.estudiantes().getFirst().porcentajeAsistencia());
    assertNull(cuadro.estudiantes().getFirst().beneficioInstitucional());
    assertTrue(cuadro.disclaimer().contains("no una escala oficial del MINEDU"));
    verify(calificaciones, times(1)).buscarPorMatriculaIds(anyCollection());
    verify(asistencias, times(1)).buscarPorMatriculaIds(anyCollection());
    verify(incidencias, times(1)).buscarPorMatriculaIds(anyCollection());
    verify(calificaciones, never()).buscarPorMatriculaId(1L);
    verify(asistencias, never()).buscarPorMatriculaId(1L);
    verify(incidencias, never()).buscarPorMatriculaId(1L);
  }

  @Test
  void docenteNoPuedeConsultarUnaSeccionSinAsignacion() {
    when(secciones.buscarPorId(102)).thenReturn(Optional.of(
        Seccion.builder().id(102).anioLectivoId((short) 2026).gradoId((short) 3).nivelId((short) 1).build()));

    assertThrows(AccessDeniedException.class, () -> service.obtenerRankingSeccion(102));
    verify(matriculas, never()).buscarPorSeccionId(102);
  }

  @Test
  void cuadroHonorDocenteSoloConsultaLasSeccionesAsignadas() {
    Seccion otraSeccion = Seccion.builder()
        .id(102).anioLectivoId((short) 2026).gradoId((short) 3).nivelId((short) 1).letra("B").build();
    when(secciones.buscarPorGradoId((short) 3)).thenReturn(List.of(seccion, otraSeccion));
    when(matriculas.buscarPorSeccionIds(eq(List.of(101))))
        .thenReturn(List.of(Matricula.builder().id(1L).estudianteId(201L).seccionId(101)
            .estadoMatricula(EstadoMatricula.MATRICULADO).build()));
    when(estudiantes.buscarPorIds(List.of(201L))).thenReturn(List.of(
        Estudiante.builder().id(201L).nombres("Ana").apellidoPaterno("Zárate").build()));
    when(calificaciones.buscarPorMatriculaIds(List.of(1L))).thenReturn(List.of(
        CalificacionCneb.builder().matriculaId(1L).calificacionCualitativa(CalificacionCualitativa.A).build()));
    when(asistencias.buscarPorMatriculaIds(List.of(1L))).thenReturn(List.of());
    when(incidencias.buscarPorMatriculaIds(List.of(1L))).thenReturn(List.of());

    var cuadro = service.obtenerCuadroHonor((short) 3, (short) 2026);

    assertEquals(1, cuadro.estudiantes().size());
    verify(matriculas).buscarPorSeccionIds(eq(List.of(101)));
    verify(matriculas, never()).buscarPorSeccionId(102);
  }

  @Test
  void cuadroHonorExigeUnSoloAnioLectivo() {
    assertThrows(BusinessException.class, () -> service.obtenerCuadroHonor((short) 3, null));
    verifyNoInteractions(secciones, matriculas);
  }

  @Test
  void puestosIgualesConCriteriosIgualesCompartenLaPosicion() {
    when(secciones.buscarPorId(101)).thenReturn(Optional.of(seccion));
    List<Matricula> lista = List.of(
        Matricula.builder().id(1L).estudianteId(201L).estadoMatricula(EstadoMatricula.MATRICULADO).build(),
        Matricula.builder().id(2L).estudianteId(202L).estadoMatricula(EstadoMatricula.MATRICULADO).build(),
        Matricula.builder().id(3L).estudianteId(203L).estadoMatricula(EstadoMatricula.MATRICULADO).build(),
        Matricula.builder().id(4L).estudianteId(204L).estadoMatricula(EstadoMatricula.MATRICULADO).build());
    when(matriculas.buscarPorSeccionId(101)).thenReturn(lista);
    when(estudiantes.buscarPorIds(List.of(201L, 202L, 203L, 204L))).thenReturn(List.of(
        Estudiante.builder().id(201L).nombres("Ana").build(),
        Estudiante.builder().id(202L).nombres("Bea").build(),
        Estudiante.builder().id(203L).nombres("Ciro").build(),
        Estudiante.builder().id(204L).nombres("Dario").build()));
    when(calificaciones.buscarPorMatriculaIds(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
        CalificacionCneb.builder().matriculaId(1L).calificacionCualitativa(CalificacionCualitativa.AD).build(),
        CalificacionCneb.builder().matriculaId(2L).calificacionCualitativa(CalificacionCualitativa.AD).build(),
        CalificacionCneb.builder().matriculaId(3L).calificacionCualitativa(CalificacionCualitativa.AD).build(),
        CalificacionCneb.builder().matriculaId(4L).calificacionCualitativa(CalificacionCualitativa.AD).build(),
        CalificacionCneb.builder().matriculaId(4L).calificacionCualitativa(CalificacionCualitativa.A).build()));
    when(asistencias.buscarPorMatriculaIds(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
        AsistenciaAula.builder().matriculaId(1L).estado(EstadoAsistenciaAula.PRESENTE).build(),
        AsistenciaAula.builder().matriculaId(2L).estado(EstadoAsistenciaAula.PRESENTE).build(),
        AsistenciaAula.builder().matriculaId(3L).estado(EstadoAsistenciaAula.PRESENTE).build(),
        AsistenciaAula.builder().matriculaId(4L).estado(EstadoAsistenciaAula.PRESENTE).build()));
    when(incidencias.buscarPorMatriculaIds(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
        IncidenciaConductual.builder().matriculaId(3L).build()));

    var estudiantesRanking = service.obtenerRankingSeccion(101).estudiantes();

    assertEquals(List.of(1, 1, 3, 4), estudiantesRanking.stream().map(x -> x.puesto()).toList());
  }
}
