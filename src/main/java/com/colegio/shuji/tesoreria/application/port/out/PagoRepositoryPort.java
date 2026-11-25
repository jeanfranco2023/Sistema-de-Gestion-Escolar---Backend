package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepositoryPort {
  PagoTransaccion guardar(PagoTransaccion valor);

  Optional<PagoTransaccion> buscarPorId(Long id);

  Optional<PagoTransaccion> bloquearPorId(Long id);

  List<PagoTransaccion> listar();

  default BigDecimal sumarAprobadosEntre(OffsetDateTime desde, OffsetDateTime hasta) {
    return null;
  }

  List<PagoTransaccion> buscarPorObligacionPagoId(Long valor);

  List<PagoTransaccion> buscarPorPasarelaTransaccionId(String valor);
}
