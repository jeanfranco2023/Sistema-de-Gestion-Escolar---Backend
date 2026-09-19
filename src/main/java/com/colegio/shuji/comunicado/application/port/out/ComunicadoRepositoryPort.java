package com.colegio.shuji.comunicado.application.port.out;

import com.colegio.shuji.comunicado.domain.model.ComunicadoOficial;
import java.util.*;

public interface ComunicadoRepositoryPort {
  ComunicadoOficial guardar(ComunicadoOficial valor);

  Optional<ComunicadoOficial> buscarPorId(Long id);

  Optional<ComunicadoOficial> bloquearPorId(Long id);

  List<ComunicadoOficial> listar();

  List<ComunicadoOficial> buscarPorRemitenteUsuarioId(Long valor);
}
