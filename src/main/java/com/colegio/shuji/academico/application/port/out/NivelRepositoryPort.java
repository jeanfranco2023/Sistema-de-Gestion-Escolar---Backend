package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.enums.*;
import com.colegio.shuji.academico.domain.model.Nivel;
import java.util.*;

public interface NivelRepositoryPort {
  Nivel guardar(Nivel valor);

  Optional<Nivel> buscarPorId(Short id);

  Optional<Nivel> bloquearPorId(Short id);

  List<Nivel> listar();

  List<Nivel> buscarPorCodigo(NivelCodigo valor);
}
