package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.Rol;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para operaciones de persistencia del catálogo de Roles. */
public interface RoleRepositoryPort {

  Optional<Rol> findById(Short id);

  Optional<Rol> findByCodigo(String codigo);

  List<Rol> findAll();

  List<Rol> findByCodigoIn(Collection<String> codigos);
}
