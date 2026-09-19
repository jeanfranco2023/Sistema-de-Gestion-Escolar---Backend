package com.colegio.shuji.usuario.application.mapper;

import com.colegio.shuji.usuario.application.dto.out.RolResponseDto;
import com.colegio.shuji.usuario.domain.model.Rol;
import java.util.Set;
import org.mapstruct.Mapper;

/** Mapper MapStruct para transformaciones del modelo Rol entre capas. */
@Mapper(componentModel = "spring")
public interface RoleMapper {

  RolResponseDto toResponseDto(Rol domain);

  Set<RolResponseDto> toResponseDtoSet(Set<Rol> domains);
}
