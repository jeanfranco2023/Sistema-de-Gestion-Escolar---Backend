package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.domain.model.Usuario;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = RolePersistenceMapper.class)
public interface UserPersistenceMapper {
  Usuario toDomain(UsuarioEntity entity);

  UsuarioEntity toEntity(Usuario domain);
}
