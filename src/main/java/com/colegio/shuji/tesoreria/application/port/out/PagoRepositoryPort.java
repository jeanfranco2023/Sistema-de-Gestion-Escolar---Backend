package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import java.util.*;

public interface PagoRepositoryPort {
  PagoTransaccion guardar(PagoTransaccion valor);

  Optional<PagoTransaccion> buscarPorId(Long id);

  Optional<PagoTransaccion> bloquearPorId(Long id);

  List<PagoTransaccion> listar();

  List<PagoTransaccion> buscarPorObligacionPagoId(Long valor);

  List<PagoTransaccion> buscarPorPasarelaTransaccionId(String valor);
}
