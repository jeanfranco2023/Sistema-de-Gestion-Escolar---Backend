package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import java.util.*;

public interface BloqueHorarioRepositoryPort {
  BloqueHorario guardar(BloqueHorario valor);

  Optional<BloqueHorario> buscarPorId(Short id);

  Optional<BloqueHorario> bloquearPorId(Short id);

  List<BloqueHorario> listar();
}
