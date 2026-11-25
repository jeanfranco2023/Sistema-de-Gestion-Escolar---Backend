package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.SessionRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Sesion;
import com.colegio.shuji.usuario.infrastructure.entity.SesionEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaSesionRepository;
import com.colegio.shuji.usuario.infrastructure.repository.JpaUsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia para el puerto SessionRepositoryPort. Conecta el dominio con Spring
 * Data JPA.
 */
@Component
@org.springframework.transaction.annotation.Transactional
@RequiredArgsConstructor
public class SessionRepositoryAdapter implements SessionRepositoryPort {
  private final com.colegio.shuji.shared.application.port.out.AuditContextPort auditoria;

  private final JpaSesionRepository jpaSesionRepository;
  private final JpaUsuarioRepository jpaUsuarioRepository;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.colegio.shuji.config.security.JwtAuthCache authCache;

  @Override
  public Sesion save(Sesion sesion) {
    auditoria.syncCurrentUserFromSecurityContext();
    UsuarioEntity usuarioEntity = jpaUsuarioRepository.getReferenceById(sesion.getUsuarioId());

    SesionEntity entity =
        SesionEntity.builder()
            .id(sesion.getId())
            .usuario(usuarioEntity)
            .tokenHash(sesion.getTokenHash())
            .ipAddress(sesion.getIpAddress())
            .userAgent(sesion.getUserAgent())
            .fechaExpiracion(sesion.getFechaExpiracion())
            .fechaCreacion(sesion.getFechaCreacion())
            .build();

    SesionEntity saved = jpaSesionRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public boolean existsById(Long id) {
    if (id == null) {
      return false;
    }
    return jpaSesionRepository.existsById(id);
  }

  @Override
  public Optional<Sesion> findByTokenHash(String tokenHash) {
    return jpaSesionRepository.findByTokenHash(tokenHash).map(this::toDomain);
  }

  @Override
  public void deleteByUsuarioId(Long usuarioId) {
    auditoria.syncCurrentUserFromSecurityContext();
    jpaSesionRepository.deleteByUsuarioId(usuarioId);
    if (authCache != null) authCache.evictAllSessions();
  }

  @Override
  public void deleteByTokenHash(String tokenHash) {
    auditoria.syncCurrentUserFromSecurityContext();
    jpaSesionRepository.deleteByTokenHash(tokenHash);
    if (authCache != null) authCache.evictAllSessions();
  }

  @Override
  public void deleteExpiredSessions(LocalDateTime now) {
    auditoria.syncCurrentUserFromSecurityContext();
    jpaSesionRepository.deleteExpiredSessions(now);
    if (authCache != null) authCache.evictAllSessions();
  }

  private Sesion toDomain(SesionEntity entity) {
    if (entity == null) {
      return null;
    }
    return Sesion.builder()
        .id(entity.getId())
        .usuarioId(entity.getUsuario() != null ? entity.getUsuario().getId() : null)
        .tokenHash(entity.getTokenHash())
        .ipAddress(entity.getIpAddress())
        .userAgent(entity.getUserAgent())
        .fechaExpiracion(entity.getFechaExpiracion())
        .fechaCreacion(entity.getFechaCreacion())
        .build();
  }
}
