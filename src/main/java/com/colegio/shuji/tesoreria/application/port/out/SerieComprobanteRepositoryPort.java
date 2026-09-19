package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.model.SerieComprobante;
import java.util.Optional;

public interface SerieComprobanteRepositoryPort {
  Optional<SerieComprobante> bloquearPorSerie(String serie);

  SerieComprobante guardar(SerieComprobante serie);
}
