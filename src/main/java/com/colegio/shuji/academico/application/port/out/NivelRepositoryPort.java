package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.domain.model.Nivel;
import java.util.List;
import java.util.Optional;

public interface NivelRepositoryPort {
  Nivel guardar(Nivel valor);

  Optional<Nivel> buscarPorId(Short id);

  Optional<Nivel> bloquearPorId(Short id);

  List<Nivel> listar();

  List<Nivel> buscarPorCodigo(NivelCodigo valor);
}
