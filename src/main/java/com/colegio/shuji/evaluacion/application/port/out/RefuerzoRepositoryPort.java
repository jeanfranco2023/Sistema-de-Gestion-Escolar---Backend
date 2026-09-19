package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import java.util.List;
import java.util.Optional;

public interface RefuerzoRepositoryPort {
  SesionRefuerzo guardar(SesionRefuerzo valor);

  Optional<SesionRefuerzo> buscarPorId(Long id);

  Optional<SesionRefuerzo> bloquearPorId(Long id);

  List<SesionRefuerzo> listar();

  List<SesionRefuerzo> buscarPorAnioLectivoId(Short valor);

  List<SesionRefuerzo> buscarPorPeriodoAcademicoId(Short valor);

  List<SesionRefuerzo> buscarPorAreaCurricularId(Short valor);

  List<SesionRefuerzo> buscarPorDocenteUsuarioId(Long valor);
}
