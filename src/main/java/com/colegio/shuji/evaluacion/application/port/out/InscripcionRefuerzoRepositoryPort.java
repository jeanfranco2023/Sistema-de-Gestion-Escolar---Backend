package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import java.util.List;
import java.util.Optional;

public interface InscripcionRefuerzoRepositoryPort {
  InscripcionRefuerzo guardar(InscripcionRefuerzo valor);

  Optional<InscripcionRefuerzo> buscarPorId(Long id);

  Optional<InscripcionRefuerzo> bloquearPorId(Long id);

  List<InscripcionRefuerzo> listar();

  List<InscripcionRefuerzo> buscarPorSesionRefuerzoId(Long valor);

  default List<InscripcionRefuerzo> buscarPorSesionRefuerzoIds(
      java.util.Collection<Long> sesionIds) {
    if (sesionIds == null || sesionIds.isEmpty()) return List.of();
    return sesionIds.stream().flatMap(id -> buscarPorSesionRefuerzoId(id).stream()).toList();
  }

  List<InscripcionRefuerzo> buscarPorEstudianteId(Long valor);

  List<InscripcionRefuerzo> buscarPorCalificacionOrigenId(Long valor);
}
