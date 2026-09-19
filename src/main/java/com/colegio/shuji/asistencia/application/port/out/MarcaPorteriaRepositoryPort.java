package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import java.util.*;

public interface MarcaPorteriaRepositoryPort {
  MarcaPorteria guardar(MarcaPorteria valor);

  Optional<MarcaPorteria> buscarPorId(Long id);

  Optional<MarcaPorteria> bloquearPorId(Long id);

  List<MarcaPorteria> listar();

  List<MarcaPorteria> buscarPorLoteId(Long valor);

  List<MarcaPorteria> buscarPorEstudianteId(Long valor);
 
  List<MarcaPorteria> buscarPorRangoFecha(java.time.OffsetDateTime inicio, java.time.OffsetDateTime fin);
}
