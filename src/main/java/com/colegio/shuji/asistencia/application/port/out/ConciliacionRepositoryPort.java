package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import java.time.*;
import java.util.*;

public interface ConciliacionRepositoryPort {
  ConciliacionAsistencia guardar(ConciliacionAsistencia valor);

  Optional<ConciliacionAsistencia> buscarPorId(Long id);

  Optional<ConciliacionAsistencia> bloquearPorId(Long id);

  List<ConciliacionAsistencia> listar();

  List<ConciliacionAsistencia> buscarPorFecha(LocalDate valor);

  List<ConciliacionAsistencia> buscarPorEstudianteId(Long valor);
}
