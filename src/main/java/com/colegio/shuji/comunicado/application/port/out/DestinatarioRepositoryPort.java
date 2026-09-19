package com.colegio.shuji.comunicado.application.port.out;

import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import java.util.*;

public interface DestinatarioRepositoryPort {
  ComunicadoDestinatario guardar(ComunicadoDestinatario valor);

  Optional<ComunicadoDestinatario> buscarPorId(Long id);

  Optional<ComunicadoDestinatario> bloquearPorId(Long id);

  List<ComunicadoDestinatario> listar();

  List<ComunicadoDestinatario> buscarPorComunicadoId(Long valor);

  List<ComunicadoDestinatario> buscarPorApoderadoId(Long valor);
}
