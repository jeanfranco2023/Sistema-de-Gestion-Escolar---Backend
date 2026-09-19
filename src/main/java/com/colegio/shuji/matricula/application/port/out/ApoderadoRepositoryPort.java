package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.domain.model.Apoderado;
import java.util.*;

public interface ApoderadoRepositoryPort {
  Apoderado guardar(Apoderado valor);

  Optional<Apoderado> buscarPorId(Long id);

  Optional<Apoderado> bloquearPorId(Long id);

  List<Apoderado> listar();

  List<Apoderado> buscarPorUsuarioId(Long valor);

  List<Apoderado> buscarPorNumeroDocumento(String valor);

  List<Apoderado> buscarPorIds(Collection<Long> ids);
}
