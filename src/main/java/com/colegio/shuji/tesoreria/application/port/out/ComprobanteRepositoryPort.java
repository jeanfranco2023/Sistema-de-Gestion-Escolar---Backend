package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepositoryPort {
  ComprobantePago guardar(ComprobantePago valor);

  Optional<ComprobantePago> buscarPorId(Long id);

  Optional<ComprobantePago> bloquearPorId(Long id);

  List<ComprobantePago> listar();

  List<ComprobantePago> buscarPorPagoTransaccionId(Long valor);

  Optional<Integer> obtenerUltimoCorrelativo(String serie);
}
