package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import java.util.List;
import java.util.Optional;

public interface CalificacionRepositoryPort {
  CalificacionCneb guardar(CalificacionCneb valor);

  Optional<CalificacionCneb> buscarPorId(Long id);

  Optional<CalificacionCneb> bloquearPorId(Long id);

  List<CalificacionCneb> listar();

  List<CalificacionCneb> buscarPorMatriculaId(Long valor);

  List<CalificacionCneb> buscarPorAnioLectivoId(Short valor);

  List<CalificacionCneb> buscarPorSeccionId(Integer valor);

  List<CalificacionCneb> buscarPorPeriodoAcademicoId(Short valor);

  List<CalificacionCneb> buscarPorAsignacionDocenteId(Long valor);

  List<CalificacionCneb> buscarPorAreaCurricularId(Short valor);

  List<CalificacionCneb> buscarPorDocenteUsuarioId(Long valor);

  List<CalificacionCneb> buscarPorCompetenciaId(Short valor);

  List<CalificacionCneb> buscarPorAsignacionYPeriodo(Long asigId, Short periodoId);
}
