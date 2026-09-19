package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import java.util.*;

public interface HorarioRepositoryPort {
  HorarioSeccion guardar(HorarioSeccion valor);

  Optional<HorarioSeccion> buscarPorId(Long id);

  Optional<HorarioSeccion> bloquearPorId(Long id);

  List<HorarioSeccion> listar();

  List<HorarioSeccion> buscarPorAnioLectivoId(Short valor);

  List<HorarioSeccion> buscarPorSeccionId(Integer valor);

  List<HorarioSeccion> buscarPorDocenteUsuarioId(Long valor);

  List<HorarioSeccion> buscarPorAsignacionDocenteId(Long valor);

  List<HorarioSeccion> buscarPorBloqueHorarioId(Short valor);
}
