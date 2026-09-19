package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import java.util.*;

public interface InscripcionRefuerzoRepositoryPort {
  InscripcionRefuerzo guardar(InscripcionRefuerzo valor);

  Optional<InscripcionRefuerzo> buscarPorId(Long id);

  Optional<InscripcionRefuerzo> bloquearPorId(Long id);

  List<InscripcionRefuerzo> listar();

  List<InscripcionRefuerzo> buscarPorSesionRefuerzoId(Long valor);

  List<InscripcionRefuerzo> buscarPorEstudianteId(Long valor);

  List<InscripcionRefuerzo> buscarPorCalificacionOrigenId(Long valor);
}
