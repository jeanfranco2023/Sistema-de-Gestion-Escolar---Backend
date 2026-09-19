package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolePersistenceMapper {
  Rol toDomain(RolEntity entity);

  RolEntity toEntity(Rol domain);

  List<Rol> toDomainList(List<RolEntity> entities);

  Set<Rol> toDomainSet(Set<RolEntity> entities);

  Set<RolEntity> toEntitySet(Set<Rol> domains);
}
