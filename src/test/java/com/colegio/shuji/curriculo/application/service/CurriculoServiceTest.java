package com.colegio.shuji.curriculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import com.colegio.shuji.curriculo.application.mapper.CurriculoMapper;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.BloqueHorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.DocenteHabilitadoPort;
import com.colegio.shuji.curriculo.application.port.out.HorarioRepositoryPort;
import com.colegio.shuji.curriculo.domain.exception.ColisionHorarioDocenteException;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CurriculoServiceTest {
  @Mock private CurriculoMapper mapper;
  @Mock private CurriculoRepositoryPort areas;
  @Mock private CompetenciaRepositoryPort competencias;
  @Mock private BloqueHorarioRepositoryPort bloques;
  @Mock private AsignacionRepositoryPort asignaciones;
  @Mock private SeccionRepositoryPort secciones;
  @Mock private AnioLectivoRepositoryPort anios;
  @Mock private NivelRepositoryPort niveles;
  @Mock private DocenteHabilitadoPort docentes;
  @Mock private ActorActualPort actor;
  @Mock private HorarioRepositoryPort horarios;

  private CurriculoService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        new CurriculoService(
            mapper, areas, competencias, bloques, asignaciones, secciones, anios, niveles,
            docentes, actor);
    service.setHorarios(horarios);
  }

  @Test
  void listarAsignacionesFiltraPorAnioSinMezclarPeriodos() {
    when(actor.tieneRol("DIRECCION")).thenReturn(true);
    var a2026 =
        AsignacionDocente.builder()
            .id(1L)
            .anioLectivoId((short) 1)
            .docenteUsuarioId(10L)
            .seccionId(5)
            .nivelId((short) 2)
            .areaCurricularId((short) 3)
            .build();
    when(asignaciones.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(a2026));
    when(mapper.toResponse(a2026))
        .thenReturn(new AsignacionDocenteResponseDto(1L, 10L, 5, (short) 1, (short) 2, (short) 3));

    var res = service.listarAsignaciones((short) 1);

    assertEquals(1, res.size());
    assertEquals((short) 1, res.get(0).anioLectivoId());
  }

  @Test
  void reasignarDocenteActualizaCuandoNoHayColisionYRechazaSiColisionaHorario() {
    var actual =
        AsignacionDocente.builder()
            .id(50L)
            .docenteUsuarioId(10L)
            .seccionId(5)
            .anioLectivoId((short) 2)
            .nivelId((short) 2)
            .areaCurricularId((short) 3)
            .build();
    var anio = AnioLectivo.builder().id((short) 2).anio((short) 2027).abierto(true).build();
    when(asignaciones.bloquearPorId(50L)).thenReturn(Optional.of(actual));
    when(anios.bloquearPorId((short) 2)).thenReturn(Optional.of(anio));
    when(docentes.listarPorArea((short) 3))
        .thenReturn(
            List.of(
                new DocenteOpcionResponseDto(10L, "doc1", "Docente 1"),
                new DocenteOpcionResponseDto(20L, "doc2", "Docente 2")));

    var bloque =
        BloqueHorario.builder()
            .id((short) 1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .esRecreo(false)
            .build();
    when(bloques.listar()).thenReturn(List.of(bloque));
    when(horarios.buscarPorAnioLectivoId((short) 2))
        .thenReturn(
            List.of(
                HorarioSeccion.builder()
                    .id(100L)
                    .anioLectivoId((short) 2)
                    .asignacionDocenteId(50L)
                    .docenteUsuarioId(10L)
                    .diaSemana((short) 1)
                    .bloqueHorarioId((short) 1)
                    .activo(true)
                    .build(),
                HorarioSeccion.builder()
                    .id(101L)
                    .anioLectivoId((short) 2)
                    .asignacionDocenteId(99L)
                    .docenteUsuarioId(20L)
                    .diaSemana((short) 1)
                    .bloqueHorarioId((short) 1)
                    .activo(true)
                    .build()));

    assertThrows(
        ColisionHorarioDocenteException.class, () -> service.reasignarDocente(50L, 20L));

    when(horarios.buscarPorAnioLectivoId((short) 2))
        .thenReturn(
            List.of(
                HorarioSeccion.builder()
                    .id(100L)
                    .anioLectivoId((short) 2)
                    .asignacionDocenteId(50L)
                    .docenteUsuarioId(10L)
                    .diaSemana((short) 1)
                    .bloqueHorarioId((short) 1)
                    .activo(true)
                    .build()));
    when(asignaciones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(AsignacionDocente.class)))
        .thenAnswer(
            inv -> {
              AsignacionDocente a = inv.getArgument(0);
              return new AsignacionDocenteResponseDto(
                  a.getId(),
                  a.getDocenteUsuarioId(),
                  a.getSeccionId(),
                  a.getAnioLectivoId(),
                  a.getNivelId(),
                  a.getAreaCurricularId());
            });

    var actualizado = service.reasignarDocente(50L, 20L);
    assertEquals(20L, actualizado.docenteUsuarioId());
    verify(asignaciones).guardar(actual);
  }
}
