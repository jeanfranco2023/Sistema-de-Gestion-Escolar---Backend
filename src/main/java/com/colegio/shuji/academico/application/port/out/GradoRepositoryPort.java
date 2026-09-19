package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.model.Grado;
import java.util.List;
import java.util.Optional;

public interface GradoRepositoryPort {
  Grado guardar(Grado valor);

  Optional<Grado> buscarPorId(Short id);

  Optional<Grado> bloquearPorId(Short id);

  List<Grado> listar();

  List<Grado> buscarPorNivelId(Short valor);
}
