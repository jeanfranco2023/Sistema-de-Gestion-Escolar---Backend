package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.ExpedienteEmpleadoDocumento;
import java.util.List;
import java.util.Optional;

public interface ExpedienteEmpleadoRepositoryPort {

  ExpedienteEmpleadoDocumento guardar(ExpedienteEmpleadoDocumento documento);

  List<ExpedienteEmpleadoDocumento> listarPorUsuarioId(Long usuarioId);

  Optional<ExpedienteEmpleadoDocumento> buscarPorIdYUsuario(Long documentoId, Long usuarioId);
}
