package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.usuario.application.dto.out.CambioAuditoriaResponseDto;
import com.colegio.shuji.usuario.application.port.in.ConsultarAuditoriaCambiosUseCase;
import com.colegio.shuji.usuario.application.port.out.AuditoriaCambiosPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultarAuditoriaCambiosService implements ConsultarAuditoriaCambiosUseCase {

  private final AuditoriaCambiosPort auditoriaCambios;

  @Override
  @Transactional(readOnly = true)
  public List<CambioAuditoriaResponseDto> listarRecientes(int limite) {
    return auditoriaCambios.listarRecientes(limite).stream()
        .map(
            cambio -> {
              boolean contextoHistoricoSinSnapshot =
                  cambio.usuarioId() != null
                      && cambio.usuarioNombre() == null
                      && cambio.roles() == null;
              String usuarioNombre =
                  cambio.usuarioNombre() != null
                      ? cambio.usuarioNombre()
                      : cambio.usuarioId() == null ? null : "Usuario #" + cambio.usuarioId();

              return new CambioAuditoriaResponseDto(
                    cambio.id(),
                    cambio.tablaAfectada(),
                    cambio.registroId(),
                    cambio.registroClave(),
                    cambio.accion(),
                    cambio.datosAnteriores(),
                    cambio.datosNuevos(),
                    cambio.usuarioId(),
                    usuarioNombre,
                    cambio.roles(),
                    contextoHistoricoSinSnapshot,
                    cambio.fechaCreacion());
            })
        .toList();
  }
}
