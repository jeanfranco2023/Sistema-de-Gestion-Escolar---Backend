package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.model.AnioLectivo;
import java.util.List;
import java.util.Optional;

public interface AnioLectivoRepositoryPort {
  AnioLectivo guardar(AnioLectivo valor);

  Optional<AnioLectivo> buscarPorId(Short id);

  Optional<AnioLectivo> bloquearPorId(Short id);

  List<AnioLectivo> listar();
}
