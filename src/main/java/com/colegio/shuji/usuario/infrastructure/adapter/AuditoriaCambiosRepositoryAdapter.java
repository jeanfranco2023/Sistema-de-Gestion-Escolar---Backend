package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.AuditoriaCambiosPort;
import com.colegio.shuji.usuario.domain.model.CambioAuditoria;
import com.colegio.shuji.usuario.infrastructure.repository.JpaAuditoriaCambiosRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditoriaCambiosRepositoryAdapter implements AuditoriaCambiosPort {

  private final JpaAuditoriaCambiosRepository repository;

  @Override
  @Transactional(readOnly = true)
  public List<CambioAuditoria> listarRecientes(int limite) {
    var pageable = PageRequest.of(0, limite);
    return repository.findAllByOrderByFechaCreacionDescIdDesc(pageable).stream()
        .map(
            evento ->
                new CambioAuditoria(
                    evento.getId(),
                    evento.getTablaAfectada(),
                    evento.getRegistroId(),
                    evento.getRegistroClave(),
                    evento.getAccion(),
                    evento.getDatosAnteriores(),
                    evento.getDatosNuevos(),
                    evento.getUsuario() == null ? null : evento.getUsuario().getId(),
                    evento.getUsuarioNombreSnapshot(),
                    evento.getRolesUsuarioSnapshot(),
                    evento.getFechaCreacion()))
        .toList();
  }
}
