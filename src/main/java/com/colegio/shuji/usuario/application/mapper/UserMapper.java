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
  @Mapping(target = "usuarioPassword", ignore = true)
  @Mapping(target = "activo", constant = "true")
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "fechaCreacion", ignore = true)
  @Mapping(target = "fechaActualizacion", ignore = true)
  @Mapping(target = "debeCambiarPassword", ignore = true)
  @Mapping(target = "telefono", ignore = true)
  @Mapping(target = "direccion", ignore = true)
  @Mapping(target = "especialidad", ignore = true)
  @Mapping(target = "codigoModular", ignore = true)
  @Mapping(target = "numeroColegiatura", ignore = true)
  @Mapping(target = "fechaIngreso", ignore = true)
  @Mapping(target = "tipoContrato", ignore = true)
  @Mapping(target = "estadoLaboral", ignore = true)
  Usuario toDomain(RegisterUserRequestDto dto);
}
