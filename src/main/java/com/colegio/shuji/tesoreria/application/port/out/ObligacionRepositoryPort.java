package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.enums.*;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.util.*;

public interface ObligacionRepositoryPort {
  ObligacionPago guardar(ObligacionPago valor);

  Optional<ObligacionPago> buscarPorId(Long id);

  Optional<ObligacionPago> bloquearPorId(Long id);

  List<ObligacionPago> listar();

  List<ObligacionPago> buscarPorMatriculaId(Long valor);

  List<ObligacionPago> buscarPorConceptoId(Short valor);

  List<ObligacionPago> buscarPorEstado(EstadoObligacion valor);
}
