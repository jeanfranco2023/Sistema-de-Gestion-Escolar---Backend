package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.enums.*;
import com.colegio.shuji.matricula.domain.model.Matricula;
import java.util.*;

public interface MatriculaRepositoryPort {
  Matricula guardar(Matricula valor);

  Optional<Matricula> buscarPorId(Long id);

  Optional<Matricula> bloquearPorId(Long id);

  List<Matricula> listar();

  List<Matricula> buscarPorAnioLectivoId(Short valor);

  List<Matricula> buscarPorEstudianteId(Long valor);

  List<Matricula> buscarPorSeccionId(Integer valor);

  List<Matricula> buscarPorEstadoMatricula(EstadoMatricula valor);

  List<Matricula> buscarPorIds(Collection<Long> ids);
}
