package com.colegio.shuji.convivencia.application.port.out;

import com.colegio.shuji.convivencia.domain.enums.*;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import java.util.*;

public interface IncidenciaRepositoryPort {
  IncidenciaConductual guardar(IncidenciaConductual valor);

  Optional<IncidenciaConductual> buscarPorId(Long id);

  Optional<IncidenciaConductual> bloquearPorId(Long id);

  List<IncidenciaConductual> listar();

  List<IncidenciaConductual> buscarPorMatriculaId(Long valor);

  List<IncidenciaConductual> buscarPorReportadoPorUsuarioId(Long valor);

  List<IncidenciaConductual> buscarPorEstado(EstadoIncidencia valor);
}
