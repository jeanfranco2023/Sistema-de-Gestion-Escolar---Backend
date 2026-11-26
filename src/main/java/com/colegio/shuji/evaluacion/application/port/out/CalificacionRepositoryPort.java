package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalificacionRepositoryPort {
  CalificacionCneb guardar(CalificacionCneb valor);

  Optional<CalificacionCneb> buscarPorId(Long id);

  default List<CalificacionCneb> buscarPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return ids.stream().map(this::buscarPorId).flatMap(Optional::stream).toList();
  }

  Optional<CalificacionCneb> bloquearPorId(Long id);

  List<CalificacionCneb> listar();

  List<CalificacionCneb> buscarPorMatriculaId(Long valor);

  List<CalificacionCneb> buscarPorMatriculaIds(Collection<Long> matriculaIds);

  List<CalificacionCneb> buscarPorAnioLectivoId(Short valor);

  List<CalificacionCneb> buscarPorSeccionId(Integer valor);

  List<CalificacionCneb> buscarPorPeriodoAcademicoId(Short valor);

  List<CalificacionCneb> buscarPorAsignacionDocenteId(Long valor);

  List<CalificacionCneb> buscarPorAreaCurricularId(Short valor);

  List<CalificacionCneb> buscarPorDocenteUsuarioId(Long valor);

  List<CalificacionCneb> buscarPorCompetenciaId(Short valor);

  List<CalificacionCneb> buscarPorAsignacionYPeriodo(Long asigId, Short periodoId);

  default List<CalificacionCneb> buscarPorAnioYPeriodo(Short anioId, Short periodoId) {
    return buscarPorAnioLectivoId(anioId).stream()
        .filter(c -> java.util.Objects.equals(c.getPeriodoAcademicoId(), periodoId))
        .toList();
  }

  default List<CalificacionCneb> buscarRiesgosPorAnio(Short anioId) {
    return buscarPorAnioLectivoId(anioId).stream()
        .filter(
            c ->
                c.necesitaRefuerzo()
                    || c.getCalificacionCualitativa()
                        == com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa.B)
        .toList();
  }

  default List<CalificacionCneb> buscarRiesgosPorAnioYPeriodo(Short anioId, Short periodoId) {
    return buscarPorAnioYPeriodo(anioId, periodoId).stream()
        .filter(
            c ->
                c.necesitaRefuerzo()
                    || c.getCalificacionCualitativa()
                        == com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa.B)
        .toList();
  }

  record ResumenNotaDashboard(
      Long matriculaId,
      Short periodoAcademicoId,
      com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa calificacionCualitativa) {}

  default Optional<List<ResumenNotaDashboard>> listarResumenDashboard(Short anioId, Short periodoId) {
    return Optional.empty();
  }

  default List<CalificacionCneb> buscarRiesgosPorPeriodo(Short periodoId) {
    return buscarPorPeriodoAcademicoId(periodoId).stream()
        .filter(
            c ->
                c.necesitaRefuerzo()
                    || c.getCalificacionCualitativa()
                        == com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa.B)
        .toList();
  }
}
