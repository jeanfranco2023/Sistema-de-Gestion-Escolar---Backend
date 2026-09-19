package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import java.util.*;

public interface ConceptoCobroRepositoryPort {
  ConceptoCobro guardar(ConceptoCobro valor);

  Optional<ConceptoCobro> buscarPorId(Short id);

  Optional<ConceptoCobro> bloquearPorId(Short id);

  List<ConceptoCobro> listar();

  List<ConceptoCobro> buscarPorCodigo(String valor);
}
