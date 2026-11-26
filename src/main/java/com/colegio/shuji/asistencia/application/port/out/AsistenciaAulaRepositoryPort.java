package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AsistenciaAulaRepositoryPort {
  AsistenciaAula guardar(AsistenciaAula valor);

  List<AsistenciaAula> guardarTodos(Collection<AsistenciaAula> valores);

  Optional<AsistenciaAula> buscarPorId(Long id);

  Optional<AsistenciaAula> bloquearPorId(Long id);

  List<AsistenciaAula> listar();

  List<AsistenciaAula> buscarPorMatriculaId(Long valor);

  List<AsistenciaAula> buscarPorMatriculaIds(Collection<Long> matriculaIds);

  List<AsistenciaAula> buscarPorFechaSesion(LocalDate valor);

  List<SeccionAsistencia> buscarSeccionesPorMatriculaIds(Collection<Long> matriculaIds);

  List<LocalDate> buscarFechasConRegistros(LocalDate desde, LocalDate hasta);

  List<AsistenciaAula> buscarPorFechaSesionYMatriculaIds(LocalDate fecha, Collection<Long> matriculaIds);

  List<AsistenciaAula> buscarPorEstado(EstadoAsistenciaAula valor);

  List<AsistenciaAula> buscarPorAuxiliarUsuarioId(Long valor);

  record ResumenConteoAsistencia(
      long presentes, long tardanzas, long faltasJustificadas, long faltasInjustificadas) {
    public long inasistencias() {
      return faltasJustificadas + faltasInjustificadas;
    }

    public long total() {
      return presentes + tardanzas + faltasJustificadas + faltasInjustificadas;
    }
  }

  default ResumenConteoAsistencia contarPorEstadoEnFecha(LocalDate fecha) {
    return null;
  }

  default ResumenConteoAsistencia contarPorEstadoYMatriculaIdsYRango(
      Collection<Long> matriculaIds, LocalDate desde, LocalDate hasta) {
    return null;
  }

  default ResumenConteoAsistencia contarPorEstadoEnAnioYRango(
      Short anioId, LocalDate desde, LocalDate hasta) {
    return null;
  }
}
