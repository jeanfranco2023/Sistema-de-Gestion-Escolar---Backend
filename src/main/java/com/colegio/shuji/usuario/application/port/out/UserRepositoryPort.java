package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para operaciones de persistencia del agregado Usuario. */
public interface UserRepositoryPort {

  Optional<Usuario> findById(Long id);

  default Optional<Usuario> obtenerPorId(Long id) {
    return findById(id);
  }

  Optional<Usuario> findByIdWithRoles(Long id);

  Optional<Usuario> findByUsername(String username);

  Optional<Usuario> findByEmail(String email);

  Optional<Usuario> findByUsernameOrEmail(String usernameOrEmail);

  List<Usuario> findByRolCodigo(String codigoRol);

  List<Usuario> findAll();

  default List<Usuario> findByIds(java.util.Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    var set = java.util.Set.copyOf(ids);
    return findAll().stream().filter(u -> set.contains(u.getId())).toList();
  }

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  boolean existsByDni(String dni);

  Usuario save(Usuario usuario);
}
