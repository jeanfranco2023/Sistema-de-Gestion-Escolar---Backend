package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.model.Aula;
import java.util.List;
import java.util.Optional;

public interface AulaRepositoryPort {
  Aula guardar(Aula aula);

  Optional<Aula> buscarPorCodigo(String codigo);

  List<Aula> listar();
}
