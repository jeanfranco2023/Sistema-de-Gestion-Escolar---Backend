package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EstudianteApoderadoRepositoryPort {
  EstudianteApoderado guardar(EstudianteApoderado valor);

  Optional<EstudianteApoderado> buscarPorId(Long id);

  Optional<EstudianteApoderado> bloquearPorId(Long id);

  List<EstudianteApoderado> listar();

  List<EstudianteApoderado> buscarPorEstudianteId(Long valor);

  List<EstudianteApoderado> buscarPorApoderadoId(Long valor);

  List<EstudianteApoderado> buscarPorEstudianteIds(Collection<Long> ids);
}
