package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para operaciones de persistencia del agregado Usuario. */
public interface UserRepositoryPort {

  Optional<Usuario> findById(Long id);

  Optional<Usuario> findByIdWithRoles(Long id);

  Optional<Usuario> findByUsername(String username);

  Optional<Usuario> findByEmail(String email);

  Optional<Usuario> findByUsernameOrEmail(String usernameOrEmail);

  List<Usuario> findByRolCodigo(String codigoRol);

  List<Usuario> findAll();

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  Usuario save(Usuario usuario);
}
