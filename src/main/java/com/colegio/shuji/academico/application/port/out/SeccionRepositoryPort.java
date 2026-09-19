package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.model.Seccion;
import java.util.*;

public interface SeccionRepositoryPort {
  Seccion guardar(Seccion valor);

  Optional<Seccion> buscarPorId(Integer id);

  Optional<Seccion> bloquearPorId(Integer id);

  List<Seccion> bloquearPorIds(Collection<Integer> ids);

  List<Seccion> listar();

  List<Seccion> buscarPorAnioLectivoId(Short valor);

  List<Seccion> buscarPorGradoId(Short valor);

  List<Seccion> buscarPorNivelId(Short valor);
}
