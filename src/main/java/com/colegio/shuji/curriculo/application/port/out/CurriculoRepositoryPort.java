package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import java.util.*;

public interface CurriculoRepositoryPort {
  AreaCurricular guardar(AreaCurricular valor);

  Optional<AreaCurricular> buscarPorId(Short id);

  Optional<AreaCurricular> bloquearPorId(Short id);

  List<AreaCurricular> listar();

  List<AreaCurricular> buscarPorNivelId(Short valor);

  List<AreaCurricular> buscarPorCodigo(String valor);
}
