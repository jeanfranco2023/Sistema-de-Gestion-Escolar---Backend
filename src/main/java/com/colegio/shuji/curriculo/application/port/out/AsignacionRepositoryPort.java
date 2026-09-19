package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import java.util.*;

public interface AsignacionRepositoryPort {
  AsignacionDocente guardar(AsignacionDocente valor);

  Optional<AsignacionDocente> buscarPorId(Long id);

  Optional<AsignacionDocente> bloquearPorId(Long id);

  List<AsignacionDocente> listar();

  List<AsignacionDocente> buscarPorDocenteUsuarioId(Long valor);

  List<AsignacionDocente> buscarPorSeccionId(Integer valor);

  List<AsignacionDocente> buscarPorAnioLectivoId(Short valor);

  List<AsignacionDocente> buscarPorNivelId(Short valor);

  List<AsignacionDocente> buscarPorAreaCurricularId(Short valor);
}
