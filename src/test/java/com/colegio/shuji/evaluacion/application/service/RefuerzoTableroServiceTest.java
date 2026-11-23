package com.colegio.shuji.evaluacion.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.AulaRepositoryPort;
import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.evaluacion.application.mapper.EvaluacionMapper;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefuerzoTableroServiceTest {
  @Mock private EvaluacionMapper mapper;
  @Mock private RefuerzoRepositoryPort refuerzos;
  @Mock private InscripcionRefuerzoRepositoryPort inscripciones;
  @Mock private CalificacionRepositoryPort calificaciones;
  @Mock private MatriculaRepositoryPort matriculas;
  @Mock private PeriodoRepositoryPort periodos;
  @Mock private AulaRepositoryPort aulas;
  @Mock private AnioLectivoRepositoryPort anios;
  @Mock private AsignacionRepositoryPort asignaciones;
  @Mock private ActorActualPort actor;
  @Mock private EstudianteRepositoryPort estudiantes;
  @Mock private SeccionRepositoryPort secciones;
  @Mock private GradoRepositoryPort grados;
  @Mock private NivelRepositoryPort niveles;
  @Mock private CurriculoRepositoryPort curriculos;
  @Mock private CompetenciaRepositoryPort competencias;
  @Mock private UserRepositoryPort usuarios;

  private RefuerzoService service;

  @BeforeEach
  void init() {
    service =
        new RefuerzoService(
            mapper,
            refuerzos,
            inscripciones,
            calificaciones,
            matriculas,
            periodos,
            aulas,
            anios,
            asignaciones,
            actor,
            estudiantes,
            secciones,
            grados,
            niveles,
            curriculos,
            competencias,
            usuarios);
  }

  private void mockCatalogoBase() {
    when(periodos.buscarPorAnioLectivoId((short) 1))
        .thenReturn(
            List.of(
                PeriodoAcademico.builder()
                    .id((short) 4)
                    .anioLectivoId((short) 1)
                    .numeroPeriodo((short) 4)
                    .nombre("IV Bimestre")
                    .activo(true)
                    .build()));
    when(curriculos.listar())
        .thenReturn(
            List.of(
                AreaCurricular.builder().id((short) 10).codigo("MAT").nombre("Matemática").build()));
    when(competencias.listar())
        .thenReturn(
            List.of(
                Competencia.builder()
                    .id((short) 50)
                    .areaId((short) 10)
                    .nombre("Resuelve problemas de cantidad")
                    .build()));
    lenient()
        .when(usuarios.findByIds(anyCollection()))
        .thenReturn(
            List.of(
                Usuario.builder()
                    .id(63L)
                    .username("alfredo.robles.pizarro")
                    .nombreCompleto("Alfredo Robles Pizarro")
                    .build(),
                Usuario.builder().id(64L).username("docente.comunicacion").build()));
    when(secciones.buscarPorAnioLectivoId((short) 1))
        .thenReturn(
            List.of(
                Seccion.builder()
                    .id(101)
                    .anioLectivoId((short) 1)
                    .nivelId((short) 3)
                    .gradoId((short) 5)
                    .letra("A")
                    .codigoAula("SEC-5-A")
                    .build(),
                Seccion.builder()
                    .id(102)
                    .anioLectivoId((short) 1)
                    .nivelId((short) 3)
                    .gradoId((short) 5)
                    .letra("B")
                    .codigoAula("SEC-5-B")
                    .build()));
    when(grados.listar())
        .thenReturn(List.of(Grado.builder().id((short) 5).nivelId((short) 3).nombre("5to").build()));
    when(niveles.listar())
        .thenReturn(
            List.of(Nivel.builder().id((short) 3).codigo(NivelCodigo.SECUNDARIA).nombre("Secundaria").build()));
  }

  @Test
  void tableroEnriqueceSesionesYRiesgosParaGestionGeneral() {
    when(actor.tieneRol("DIRECCION")).thenReturn(false);
    when(actor.tieneRol("ADMINISTRADOR")).thenReturn(true);
    when(anios.listar())
        .thenReturn(List.of(AnioLectivo.builder().id((short) 1).anio((short) 2026).abierto(true).build()));
    mockCatalogoBase();

    var sesion1 =
        SesionRefuerzo.builder()
            .id(1000L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 4)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(63L)
            .tema("Ecuaciones lineales")
            .fechaProgramada(LocalDate.of(2026, 11, 10))
            .horaInicio(LocalTime.of(15, 0))
            .horaFin(LocalTime.of(16, 0))
            .aulaAsignada("SEC-5-A")
            .build();
    when(refuerzos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(sesion1));
    when(inscripciones.buscarPorSesionRefuerzoIds(List.of(1000L)))
        .thenReturn(
            List.of(
                InscripcionRefuerzo.builder()
                    .id(500L)
                    .sesionRefuerzoId(1000L)
                    .estudianteId(201L)
                    .calificacionOrigenId(301L)
                    .estadoAsistencia(EstadoAsistenciaRefuerzo.ASISTIO)
                    .build()));
    when(calificaciones.buscarRiesgosPorAnio((short) 1))
        .thenReturn(
            List.of(
                CalificacionCneb.builder()
                    .id(301L)
                    .matriculaId(401L)
                    .anioLectivoId((short) 1)
                    .periodoAcademicoId((short) 4)
                    .areaCurricularId((short) 10)
                    .docenteUsuarioId(63L)
                    .competenciaId((short) 50)
                    .calificacionCualitativa(CalificacionCualitativa.C)
                    .conclusionDescriptiva("Requiere apoyo en ecuaciones")
                    .build(),
                CalificacionCneb.builder()
                    .id(302L)
                    .matriculaId(402L)
                    .anioLectivoId((short) 1)
                    .periodoAcademicoId((short) 4)
                    .areaCurricularId((short) 10)
                    .docenteUsuarioId(63L)
                    .competenciaId((short) 50)
                    .calificacionCualitativa(CalificacionCualitativa.B)
                    .conclusionDescriptiva("En proceso")
                    .build()));
    when(matriculas.buscarPorIds(anyCollection()))
        .thenReturn(
            List.of(
                Matricula.builder()
                    .id(401L)
                    .anioLectivoId((short) 1)
                    .estudianteId(201L)
                    .seccionId(101)
                    .estadoMatricula(EstadoMatricula.MATRICULADO)
                    .build(),
                Matricula.builder()
                    .id(402L)
                    .anioLectivoId((short) 1)
                    .estudianteId(202L)
                    .seccionId(102)
                    .estadoMatricula(EstadoMatricula.MATRICULADO)
                    .build()));
    when(estudiantes.buscarPorIds(anyCollection()))
        .thenReturn(
            List.of(
                Estudiante.builder()
                    .id(201L)
                    .numeroDocumento("70000001")
                    .nombres("Lucia")
                    .apellidoPaterno("Perez")
                    .apellidoMaterno("Gomez")
                    .build(),
                Estudiante.builder()
                    .id(202L)
                    .numeroDocumento("70000002")
                    .nombres("Mateo")
                    .apellidoPaterno("Quispe")
                    .apellidoMaterno("Rojas")
                    .build()));

    var res = service.tablero(null, null);

    assertEquals(1, res.totalSesiones());
    assertEquals(1, res.totalInscripciones());
    assertEquals(1, res.asistieron());
    assertEquals(2, res.evaluacionesRiesgoTotal());
    assertEquals(1, res.evaluacionesSinDerivar());

    var sd = res.sesiones().getFirst();
    assertEquals("Alfredo Robles Pizarro", sd.docenteNombre());
    assertEquals("5to Secundaria · A", sd.gradoSeccion());
    assertEquals(1, sd.casosC());
    assertEquals("COMPLETADA", sd.estadoSesion());

    // El caso sin derivar aparece primero
    assertFalse(res.riesgos().get(0).derivado());
    assertEquals("Mateo Quispe Rojas", res.riesgos().get(0).nombreEstudiante());
    assertTrue(res.riesgos().get(1).derivado());
    assertEquals("ASISTIO", res.riesgos().get(1).estadoAsistencia());
  }

  @Test
  void tableroFiltraVisibilidadCuandoActorEsDocente() {
    when(actor.tieneRol("DIRECCION")).thenReturn(false);
    when(actor.tieneRol("ADMINISTRADOR")).thenReturn(false);
    when(actor.usuarioId()).thenReturn(63L);
    when(periodos.buscarPorId((short) 4))
        .thenReturn(Optional.of(PeriodoAcademico.builder().id((short) 4).anioLectivoId((short) 1).build()));
    mockCatalogoBase();

    var mia =
        SesionRefuerzo.builder()
            .id(1L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 4)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(63L)
            .fechaProgramada(LocalDate.of(2026, 11, 10))
            .horaInicio(LocalTime.of(15, 0))
            .horaFin(LocalTime.of(16, 0))
            .build();
    var otra =
        SesionRefuerzo.builder()
            .id(2L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 4)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(64L)
            .fechaProgramada(LocalDate.of(2026, 11, 10))
            .horaInicio(LocalTime.of(16, 0))
            .horaFin(LocalTime.of(17, 0))
            .build();
    when(refuerzos.buscarPorPeriodoAcademicoId((short) 4)).thenReturn(List.of(mia, otra));
    when(inscripciones.buscarPorSesionRefuerzoIds(List.of(1L))).thenReturn(List.of());
    when(calificaciones.buscarRiesgosPorAnioYPeriodo((short) 1, (short) 4)).thenReturn(List.of());

    var res = service.tablero(null, (short) 4);

    assertEquals(1, res.sesiones().size());
    assertEquals(1L, res.sesiones().getFirst().id());
  }

  @Test
  void derivarConAulaDeSeccionSoloIncluyeEstudiantesDeEsaSeccion() {
    var sesion =
        SesionRefuerzo.builder()
            .id(10L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 4)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(63L)
            .aulaAsignada("SEC-5-A")
            .build();
    when(refuerzos.bloquearPorId(10L)).thenReturn(Optional.of(sesion));
    when(inscripciones.buscarPorSesionRefuerzoId(10L)).thenReturn(List.of());
    when(secciones.buscarPorAnioLectivoId((short) 1))
        .thenReturn(
            List.of(
                Seccion.builder().id(101).codigoAula("SEC-5-A").build(),
                Seccion.builder().id(102).codigoAula("SEC-5-B").build()));
    when(calificaciones.buscarRiesgosPorPeriodo((short) 4))
        .thenReturn(
            List.of(
                CalificacionCneb.builder()
                    .id(1L)
                    .matriculaId(11L)
                    .areaCurricularId((short) 10)
                    .docenteUsuarioId(63L)
                    .calificacionCualitativa(CalificacionCualitativa.C)
                    .build(),
                CalificacionCneb.builder()
                    .id(2L)
                    .matriculaId(12L)
                    .areaCurricularId((short) 10)
                    .docenteUsuarioId(63L)
                    .calificacionCualitativa(CalificacionCualitativa.C)
                    .build()));
    when(matriculas.buscarPorIds(List.of(11L, 12L)))
        .thenReturn(
            List.of(
                Matricula.builder()
                    .id(11L)
                    .estudianteId(21L)
                    .seccionId(101)
                    .estadoMatricula(EstadoMatricula.MATRICULADO)
                    .build(),
                Matricula.builder()
                    .id(12L)
                    .estudianteId(22L)
                    .seccionId(102)
                    .estadoMatricula(EstadoMatricula.MATRICULADO)
                    .build()));
    when(inscripciones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(InscripcionRefuerzo.class)))
        .thenReturn(mock(com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto.class));

    var res = service.derivar(10L);

    assertEquals(1, res.size());
    verify(inscripciones, times(1)).guardar(argThat(i -> i.getEstudianteId().equals(21L)));
  }

  @Test
  void inscripcionesDevuelveDetalleEnriquecido() {
    var sesion =
        SesionRefuerzo.builder()
            .id(1000L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 4)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(63L)
            .aulaAsignada("SEC-5-A")
            .build();
    when(refuerzos.buscarPorId(1000L)).thenReturn(Optional.of(sesion));
    when(inscripciones.buscarPorSesionRefuerzoId(1000L))
        .thenReturn(
            List.of(
                InscripcionRefuerzo.builder()
                    .id(500L)
                    .sesionRefuerzoId(1000L)
                    .estudianteId(201L)
                    .calificacionOrigenId(301L)
                    .estadoAsistencia(EstadoAsistenciaRefuerzo.ASISTIO)
                    .observaciones("Avance positivo")
                    .build()));
    when(estudiantes.buscarPorIds(List.of(201L)))
        .thenReturn(
            List.of(
                Estudiante.builder()
                    .id(201L)
                    .numeroDocumento("70000001")
                    .nombres("Lucia")
                    .apellidoPaterno("Perez")
                    .apellidoMaterno("Gomez")
                    .build()));
    when(calificaciones.buscarPorIds(List.of(301L)))
        .thenReturn(
            List.of(
                CalificacionCneb.builder()
                    .id(301L)
                    .matriculaId(401L)
                    .competenciaId((short) 50)
                    .calificacionCualitativa(CalificacionCualitativa.C)
                    .conclusionDescriptiva("Requiere apoyo en ecuaciones")
                    .build()));
    when(matriculas.buscarPorIds(List.of(401L)))
        .thenReturn(List.of(Matricula.builder().id(401L).seccionId(101).build()));
    when(competencias.buscarPorIds(anyCollection()))
        .thenReturn(
            List.of(
                Competencia.builder()
                    .id((short) 50)
                    .areaId((short) 10)
                    .nombre("Resuelve problemas de cantidad")
                    .build()));
    when(periodos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
    when(curriculos.listar()).thenReturn(List.of());
    when(usuarios.findAll()).thenReturn(List.of());
    when(secciones.buscarPorAnioLectivoId((short) 1))
        .thenReturn(
            List.of(
                Seccion.builder()
                    .id(101)
                    .anioLectivoId((short) 1)
                    .nivelId((short) 3)
                    .gradoId((short) 5)
                    .letra("A")
                    .codigoAula("SEC-5-A")
                    .build()));
    when(grados.listar())
        .thenReturn(List.of(Grado.builder().id((short) 5).nivelId((short) 3).nombre("5to").build()));
    when(niveles.listar())
        .thenReturn(
            List.of(
                Nivel.builder().id((short) 3).codigo(NivelCodigo.SECUNDARIA).nombre("Secundaria").build()));

    var res = service.inscripciones(1000L);

    assertEquals(1, res.size());
    var item = res.getFirst();
    assertEquals("70000001", item.dniEstudiante());
    assertEquals("5to Secundaria · A", item.gradoSeccion());
    assertEquals("C", item.calificacionOrigen());
    assertEquals("Resuelve problemas de cantidad", item.competenciaNombre());
    assertEquals("Requiere apoyo en ecuaciones", item.conclusionOrigen());
  }

  @Test
  void rechazaProgramarODerivarEnBimestrePrevioConcluidoYFiltraTableroSinPeriodo() {
    var hoy = LocalDate.now(java.time.ZoneId.of("America/Lima"));
    var b1 =
        PeriodoAcademico.builder()
            .id((short) 1)
            .anioLectivoId((short) 1)
            .numeroPeriodo((short) 1)
            .nombre("I Bimestre")
            .fechaInicio(hoy.minusMonths(5))
            .fechaFin(hoy.minusMonths(3))
            .cerrado(false)
            .activo(true)
            .build();
    var b4 =
        PeriodoAcademico.builder()
            .id((short) 4)
            .anioLectivoId((short) 1)
            .numeroPeriodo((short) 4)
            .nombre("IV Bimestre")
            .fechaInicio(hoy.minusDays(10))
            .fechaFin(hoy.plusDays(20))
            .cerrado(false)
            .activo(true)
            .build();

    when(anios.bloquearPorId((short) 1))
        .thenReturn(Optional.of(AnioLectivo.builder().id((short) 1).anio((short) 2026).abierto(true).build()));
    when(periodos.bloquearPorId((short) 1)).thenReturn(Optional.of(b1));
    when(periodos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(b1, b4));

    var req =
        new com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto(
            (short) 1,
            (short) 1,
            (short) 10,
            63L,
            "Refuerzo B1",
            hoy.plusDays(1),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            "SEC-5-A");

    assertThrows(
        com.colegio.shuji.shared.domain.exception.BusinessException.class,
        () -> service.programar(req));

    var sesionB1 =
        SesionRefuerzo.builder()
            .id(11L)
            .anioLectivoId((short) 1)
            .periodoAcademicoId((short) 1)
            .areaCurricularId((short) 10)
            .docenteUsuarioId(63L)
            .fechaProgramada(hoy.minusMonths(3))
            .horaInicio(LocalTime.of(15, 0))
            .horaFin(LocalTime.of(16, 0))
            .build();
    when(refuerzos.bloquearPorId(11L)).thenReturn(Optional.of(sesionB1));
    when(periodos.buscarPorId((short) 1)).thenReturn(Optional.of(b1));

    assertThrows(
        com.colegio.shuji.shared.domain.exception.BusinessException.class,
        () -> service.derivar(11L));

    when(actor.tieneRol("DIRECCION")).thenReturn(true);
    when(curriculos.listar()).thenReturn(List.of());
    when(competencias.listar()).thenReturn(List.of());
    when(secciones.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
    when(grados.listar()).thenReturn(List.of());
    when(niveles.listar()).thenReturn(List.of());
    when(refuerzos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(sesionB1));
    when(calificaciones.buscarRiesgosPorAnio((short) 1))
        .thenReturn(
            List.of(
                CalificacionCneb.builder()
                    .id(99L)
                    .matriculaId(401L)
                    .anioLectivoId((short) 1)
                    .periodoAcademicoId((short) 1)
                    .calificacionCualitativa(CalificacionCualitativa.C)
                    .build()));

    var tablero = service.tablero((short) 1, null);
    assertEquals((short) 4, tablero.periodoId());
    assertEquals(0, tablero.totalSesiones());
    assertEquals(0, tablero.evaluacionesRiesgoTotal());
  }
}
