package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.model.Estudiante;
import java.util.*;

public interface EstudianteRepositoryPort {
  Estudiante guardar(Estudiante valor);

  Optional<Estudiante> buscarPorId(Long id);

  Optional<Estudiante> bloquearPorId(Long id);

  List<Estudiante> listar();

  List<Estudiante> buscarPorNumeroDocumento(String valor);

  List<Estudiante> buscarPorNumerosDocumento(Collection<String> numeros);

  List<Estudiante> bloquearPorIds(Collection<Long> ids);
}
