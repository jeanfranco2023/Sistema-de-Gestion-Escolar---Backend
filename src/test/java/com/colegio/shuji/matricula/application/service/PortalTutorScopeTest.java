package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.matricula.application.port.out.AccesoFamiliarPort;
import com.colegio.shuji.matricula.application.port.out.ConsultaPortalDatosPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class PortalTutorScopeTest {
  @Test
  void tutorSoloRecibeMatriculasDeSeccionesAsignadasEnAnioAbierto() {
    var anioAbierto = AnioLectivo.builder().id((short) 2026).abierto(true).build();
    var asignacion = AsignacionDocente.builder().docenteUsuarioId(7L).seccionId(10).anioLectivoId((short) 2026).build();
    var asignado = hijo(10, 2026, 100L);
    var anioCerrado = hijo(10, 2025, 101L);
    var otraSeccion = hijo(11, 2026, 102L);
    var datos = port(ConsultaPortalDatosPort.class, (m, a) -> {
      if (m.getName().equals("hijosDeSecciones")) return List.of(asignado, anioCerrado, otraSeccion);
      if (m.getName().equals("hijos")) return List.of(otraSeccion);
      return null;
    });
    var servicio = new PortalFamiliarService(
        datos,
        port(AccesoFamiliarPort.class, (m, a) -> null),
        port(MatriculaRepositoryPort.class, (m, a) -> null),
        port(GestionarAnioLectivoUseCase.class, (m, a) -> null),
        port(ActorActualPort.class, (m, a) -> m.getName().equals("tieneRol") ? "TUTOR".equals(a[0]) : 7L),
        port(AsignacionRepositoryPort.class, (m, a) -> m.getName().equals("buscarPorDocenteUsuarioId") ? List.of(asignacion) : null),
        port(AnioLectivoRepositoryPort.class, (m, a) -> m.getName().equals("listar") ? List.of(anioAbierto) : null));

    assertEquals(List.of(asignado), servicio.alumnosSeguimiento());
  }

  private static HijoPortalResponseDto hijo(Integer seccionId, int anio, Long matriculaId) {
    return new HijoPortalResponseDto(200L, matriculaId, (short) anio, (short) anio, seccionId,
        "Alumno", "Apellido", "Materno", "12345678", "PRIMARIA", "1", "A");
  }

  private static <T> T port(Class<T> type, BiFunction<Method, Object[], Object> handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
        (proxy, method, args) -> {
          var result = handler.apply(method, args == null ? new Object[0] : args);
          if (result != null) return result;
          if (Optional.class.isAssignableFrom(method.getReturnType())) return Optional.empty();
          if (List.class.isAssignableFrom(method.getReturnType())) return List.of();
          if (method.getReturnType() == boolean.class) return false;
          return null;
        }));
  }
}
