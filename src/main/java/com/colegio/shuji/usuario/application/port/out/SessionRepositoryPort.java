package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.Sesion;
import java.time.LocalDateTime;
import java.util.Optional;

/** Puerto de salida para operaciones de persistencia de las Sesiones activas. */
public interface SessionRepositoryPort {

  Sesion save(Sesion sesion);

  Optional<Sesion> findByTokenHash(String tokenHash);

  void deleteByUsuarioId(Long usuarioId);

  void deleteExpiredSessions(LocalDateTime now);
}
