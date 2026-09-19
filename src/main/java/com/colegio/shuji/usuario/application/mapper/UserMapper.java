package com.colegio.shuji.usuario.application.mapper;

import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapper MapStruct para transformaciones del modelo Usuario entre capas. */
@Mapper(
    componentModel = "spring",
    uses = {RoleMapper.class})
public interface UserMapper {

  UserResponseDto toResponseDto(Usuario domain);

  List<UserResponseDto> toResponseDtoList(List<Usuario> domains);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "uuid", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "activo", constant = "true")
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Usuario toDomain(RegisterUserRequestDto dto);
}
