package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.domain.enums.*;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import java.time.*;
import java.util.*;

public interface AsistenciaAulaRepositoryPort {
  AsistenciaAula guardar(AsistenciaAula valor);

  Optional<AsistenciaAula> buscarPorId(Long id);

  Optional<AsistenciaAula> bloquearPorId(Long id);

  List<AsistenciaAula> listar();

  List<AsistenciaAula> buscarPorMatriculaId(Long valor);

  List<AsistenciaAula> buscarPorFechaSesion(LocalDate valor);

  List<AsistenciaAula> buscarPorEstado(EstadoAsistenciaAula valor);

  List<AsistenciaAula> buscarPorAuxiliarUsuarioId(Long valor);
}
