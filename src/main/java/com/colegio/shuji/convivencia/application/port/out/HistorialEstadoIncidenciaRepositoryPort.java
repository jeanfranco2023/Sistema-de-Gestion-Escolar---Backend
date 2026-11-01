package com.colegio.shuji.convivencia.application.port.out;

import com.colegio.shuji.convivencia.domain.model.CambioEstadoIncidencia;
import java.util.List;

public interface HistorialEstadoIncidenciaRepositoryPort {
  CambioEstadoIncidencia guardar(CambioEstadoIncidencia cambio);

  List<CambioEstadoIncidencia> buscarPorIncidenciaId(Long incidenciaId);
}
