package com.colegio.shuji.academico.application.port.out;

import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import java.util.*;

public interface PeriodoRepositoryPort {
  PeriodoAcademico guardar(PeriodoAcademico valor);

  Optional<PeriodoAcademico> buscarPorId(Short id);

  Optional<PeriodoAcademico> bloquearPorId(Short id);

  List<PeriodoAcademico> listar();

  List<PeriodoAcademico> buscarPorAnioLectivoId(Short valor);
}
