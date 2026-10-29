package com.colegio.shuji.asistencia.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.mapper.AsistenciaMapper;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.BiometricoRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AsistenciaDocenteSeguridadTest {
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
