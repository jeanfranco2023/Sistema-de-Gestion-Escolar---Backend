package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.Competencia;
import java.util.*;

public interface CompetenciaRepositoryPort {
  Competencia guardar(Competencia valor);

  Optional<Competencia> buscarPorId(Short id);

  Optional<Competencia> bloquearPorId(Short id);

  List<Competencia> listar();

  List<Competencia> buscarPorAreaId(Short valor);
}
