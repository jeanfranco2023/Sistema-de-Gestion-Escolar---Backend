package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.domain.model.MallaHorariaGrado;
import java.util.List;
import java.util.Optional;

public interface MallaHorariaGradoRepositoryPort {
  MallaHorariaGrado guardar(MallaHorariaGrado valor);
  Optional<MallaHorariaGrado> buscarPorId(Long id);
  Optional<MallaHorariaGrado> bloquearPorId(Long id);
  List<MallaHorariaGrado> buscarPorGradoYAnio(Short gradoId, Short anioLectivoId);
}
