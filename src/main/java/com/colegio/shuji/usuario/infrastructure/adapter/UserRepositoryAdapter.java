package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaUsuarioRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de persistencia para el puerto UserRepositoryPort. Conecta el dominio con Spring Data
 * JPA y MapStruct.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
  private final com.colegio.shuji.shared.application.port.out.AuditContextPort auditoria;

  private final JpaUsuarioRepository jpaUsuarioRepository;
  private final UserPersistenceMapper userMapper;

  @Override
  public Optional<Usuario> findById(Long id) {
    return jpaUsuarioRepository.findById(id).map(userMapper::toDomain);
  }

  @Override
  public Optional<Usuario> obtenerPorId(Long id) {
    return findById(id);
  }

  @Override
  public Optional<Usuario> findByIdWithRoles(Long id) {
    return jpaUsuarioRepository.findByIdWithRoles(id).map(userMapper::toDomain);
  }

  @Override
  public Optional<Usuario> findByUsername(String username) {
    return jpaUsuarioRepository.findByUsername(username).map(userMapper::toDomain);
  }

  @Override
  public Optional<Usuario> findByEmail(String email) {
    return jpaUsuarioRepository.findByEmail(email).map(userMapper::toDomain);
  }

  @Override
  public Optional<Usuario> findByUsernameOrEmail(String usernameOrEmail) {
    return jpaUsuarioRepository.findByUsernameOrEmail(usernameOrEmail).map(userMapper::toDomain);
  }

  @Override
  public List<Usuario> findByRolCodigo(String codigoRol) {
    return jpaUsuarioRepository.findByRolCodigo(codigoRol).stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Override
  public List<Usuario> findAll() {
    return jpaUsuarioRepository.findAllWithRoles().stream().map(userMapper::toDomain).toList();
  }

  @Override
  public boolean existsByUsername(String username) {
    return jpaUsuarioRepository.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaUsuarioRepository.existsByEmail(email);
  }

  @Override
  public Usuario save(Usuario usuario) {
    auditoria.syncCurrentUserFromSecurityContext();
    UsuarioEntity entity = userMapper.toEntity(usuario);
    UsuarioEntity saved = jpaUsuarioRepository.save(entity);
    return userMapper.toDomain(saved);
  }
}
