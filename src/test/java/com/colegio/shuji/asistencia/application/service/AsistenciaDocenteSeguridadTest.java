package com.colegio.shuji.asistencia.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.mapper.AsistenciaMapper;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.BiometricoRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.SeccionAsistencia;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.access.AccessDeniedException;

class AsistenciaDocenteSeguridadTest {
  @Test
  void reporteDiarioIncluyeSeccionesDeDistintoGradoConUnaCargaAgrupada() {
    var fecha = LocalDate.of(2027, 3, 1);
    var registros = List.of(
        AsistenciaAula.builder().id(1L).matriculaId(40L).fechaSesion(fecha).build(),
        AsistenciaAula.builder().id(2L).matriculaId(41L).fechaSesion(fecha).build());
    var matriculas = List.of(
        Matricula.builder().id(40L).estudianteId(400L).seccionId(10).build(),
        Matricula.builder().id(41L).estudianteId(401L).seccionId(20).build());
    var alumnos = List.of(
        Estudiante.builder().id(400L).nombres("Ana").apellidoPaterno("Rojas").build(),
        Estudiante.builder().id(401L).nombres("Luis").apellidoPaterno("Torres").build());
    var cargasSeccion = new AtomicInteger();
    var servicio = new AsistenciaService(
        Mappers.getMapper(AsistenciaMapper.class),
        port(BiometricoRepositoryPort.class, (m, a) -> null),
        port(MarcaPorteriaRepositoryPort.class, (m, a) -> null),
        port(EstudianteRepositoryPort.class,
            (m, a) -> m.getName().equals("buscarPorIds") ? alumnos : null),
        port(MatriculaRepositoryPort.class,
            (m, a) -> m.getName().equals("buscarPorIds") ? matriculas : null),
        port(AsistenciaAulaRepositoryPort.class, (m, a) -> {
          if (m.getName().equals("buscarPorFechaSesion")) return registros;
          if (m.getName().equals("buscarSeccionesPorMatriculaIds")) {
            cargasSeccion.incrementAndGet();
            return List.of(
                new SeccionAsistencia(40L, 10, "Primer grado", "Secundaria", "A"),
                new SeccionAsistencia(41L, 20, "Segundo grado", "Secundaria", "A"));
          }
          return null;
        }),
        port(AsignacionRepositoryPort.class, (m, a) -> null),
        port(AnioLectivoRepositoryPort.class, (m, a) -> null),
        port(ActorActualPort.class, (m, a) -> null));

    var reporte = servicio.reporte(fecha);

    assertEquals(1, cargasSeccion.get());
    assertEquals(10, reporte.asistencias().get(0).seccionId());
    assertEquals("Primer grado · Secundaria · Sección A", reporte.asistencias().get(0).seccion());
    assertEquals(20, reporte.asistencias().get(1).seccionId());
    assertEquals("Segundo grado · Secundaria · Sección A", reporte.asistencias().get(1).seccion());
  }

  @Test
  void rechazaFechaDeAsistenciaFueraDelAnioDeLaMatricula() {
    var inicio = LocalDate.of(2027, 3, 1);
    var fin = LocalDate.of(2027, 12, 15);
    var guardadas = new AtomicInteger();
    var servicio = servicioParaRegistro(matricula2027(), anio2027(inicio, fin), guardadas);

    assertThrows(
        com.colegio.shuji.shared.domain.exception.BusinessException.class,
        () -> servicio.registrar(solicitud(LocalDate.of(2026, 12, 31))));
    assertEquals(0, guardadas.get());
  }

  @Test
  void aceptaLosLimitesInclusivosDelAnioLectivoDeLaMatricula() {
    var inicio = LocalDate.of(2027, 3, 1);
    var fin = LocalDate.of(2027, 12, 15);
    for (var fecha : List.of(inicio, fin)) {
      var guardadas = new AtomicInteger();
      servicioParaRegistro(matricula2027(), anio2027(inicio, fin), guardadas)
          .registrar(solicitud(fecha));
      assertEquals(1, guardadas.get());
    }
  }

  @Test
  void docenteNoPuedeRegistrarAsistenciaEnSeccionAjena() {
    var matricula =
        Matricula.builder()
            .id(40L)
            .anioLectivoId((short) 2026)
            .estudianteId(400L)
            .seccionId(20)
            .estadoMatricula(EstadoMatricula.MATRICULADO)
            .build();
    var anio =
        com.colegio.shuji.academico.domain.model.AnioLectivo.builder()
            .id((short) 2026)
            .abierto(true)
            .build();
    var asignacion =
        AsignacionDocente.builder()
            .docenteUsuarioId(7L)
            .seccionId(10)
            .anioLectivoId((short) 2026)
            .build();
    var servicio =
        new AsistenciaService(
            port(AsistenciaMapper.class, (m, a) -> null),
            port(BiometricoRepositoryPort.class, (m, a) -> null),
            port(MarcaPorteriaRepositoryPort.class, (m, a) -> null),
            port(EstudianteRepositoryPort.class, (m, a) -> null),
            port(
                MatriculaRepositoryPort.class,
                (m, a) -> m.getName().equals("bloquearPorId") ? Optional.of(matricula) : null),
            port(AsistenciaAulaRepositoryPort.class, (m, a) -> null),
            port(
                AsignacionRepositoryPort.class,
                (m, a) -> m.getName().equals("buscarPorDocenteUsuarioId") ? List.of(asignacion) : null),
            port(AnioLectivoRepositoryPort.class, (m, a) -> m.getName().equals("listar") ? List.of(anio) : null),
            port(
                ActorActualPort.class,
                (m, a) -> {
                  if (m.getName().equals("tieneRol")) return "DOCENTE".equals(a[0]);
                  if (m.getName().equals("usuarioId")) return 7L;
                  return null;
                }));

    assertThrows(
        AccessDeniedException.class,
        () ->
            servicio.registrar(
                new RegistrarAsistenciaAulaRequestDto(
                    40L, LocalDate.now(), LocalTime.of(8, 0), EstadoAsistenciaAula.PRESENTE)));

  }

  private static <T> T port(Class<T> type, BiFunction<Method, Object[], Object> handler) {
    return type.cast(
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> {
              var result = handler.apply(method, args == null ? new Object[0] : args);
              return result != null ? result : valorPredeterminado(method.getReturnType());
            }));
  }

  private static AsistenciaService servicioParaRegistro(
      Matricula matricula, AnioLectivo anio, AtomicInteger guardadas) {
    return new AsistenciaService(
        port(
            AsistenciaMapper.class,
            (m, a) -> m.getName().equals("toDomain")
                ? AsistenciaAula.builder().estado(EstadoAsistenciaAula.PRESENTE).build()
                : null),
        port(BiometricoRepositoryPort.class, (m, a) -> null),
        port(MarcaPorteriaRepositoryPort.class, (m, a) -> null),
        port(EstudianteRepositoryPort.class, (m, a) -> null),
        port(
            MatriculaRepositoryPort.class,
            (m, a) -> m.getName().equals("bloquearPorId") ? Optional.of(matricula) : null),
        port(
            AsistenciaAulaRepositoryPort.class,
            (m, a) -> {
              if (m.getName().equals("buscarPorMatriculaId")) return List.of();
              if (m.getName().equals("guardar")) {
                guardadas.incrementAndGet();
                return a[0];
              }
              return null;
            }),
        port(AsignacionRepositoryPort.class, (m, a) -> null),
        port(
            AnioLectivoRepositoryPort.class,
            (m, a) -> m.getName().equals("buscarPorId") ? Optional.of(anio) : null),
        port(
            ActorActualPort.class,
            (m, a) -> {
              if (m.getName().equals("tieneRol")) return false;
              if (m.getName().equals("usuarioId")) return 7L;
              return null;
            }));
  }

  private static Matricula matricula2027() {
    return Matricula.builder()
        .id(40L)
        .anioLectivoId((short) 2027)
        .estudianteId(400L)
        .seccionId(20)
        .estadoMatricula(EstadoMatricula.MATRICULADO)
        .build();
  }

  private static AnioLectivo anio2027(LocalDate inicio, LocalDate fin) {
    return AnioLectivo.builder()
        .id((short) 2027)
        .anio((short) 2027)
        .fechaInicio(inicio)
        .fechaFin(fin)
        .abierto(true)
        .build();
  }

  private static RegistrarAsistenciaAulaRequestDto solicitud(LocalDate fecha) {
    return new RegistrarAsistenciaAulaRequestDto(
        40L, fecha, LocalTime.of(8, 0), EstadoAsistenciaAula.PRESENTE);
  }

  private static Object valorPredeterminado(Class<?> type) {
    if (!type.isPrimitive()) {
      if (Optional.class.isAssignableFrom(type)) return Optional.empty();
      if (List.class.isAssignableFrom(type)) return List.of();
      return null;
    }
    if (type == boolean.class) return false;
    if (type == char.class) return '\0';
    if (type == long.class) return 0L;
    if (type == double.class) return 0D;
    if (type == float.class) return 0F;
    if (type == short.class) return (short) 0;
    if (type == byte.class) return (byte) 0;
    return 0;
  }
}
