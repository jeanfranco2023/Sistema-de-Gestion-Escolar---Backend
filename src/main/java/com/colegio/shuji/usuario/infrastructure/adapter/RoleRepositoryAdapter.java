package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.RoleRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.infrastructure.repository.JpaRolRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de persistencia para el puerto RoleRepositoryPort. Conecta el dominio con Spring Data
 * JPA y MapStruct.
 */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

  private final JpaRolRepository jpaRolRepository;
  private final RolePersistenceMapper roleMapper;

  @Override
  public Optional<Rol> findById(Short id) {
    return jpaRolRepository.findById(id).map(roleMapper::toDomain);
  }

  @Override
  public Optional<Rol> findByCodigo(String codigo) {
    return jpaRolRepository.findByCodigo(codigo).map(roleMapper::toDomain);
  }

  @Override
  public List<Rol> findAll() {
    return roleMapper.toDomainList(jpaRolRepository.findAllOrderByIdAsc());
  }

  @Override
  public List<Rol> findByCodigoIn(Collection<String> codigos) {
    return roleMapper.toDomainList(jpaRolRepository.findByCodigoIn(codigos));
  }
}
