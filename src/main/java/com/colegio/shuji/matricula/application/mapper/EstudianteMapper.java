package com.colegio.shuji.matricula.application.mapper;

import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EstudianteMapper {
  @Mapping(target = "codigoInterno", ignore = true)
  @Mapping(target = "tieneApoderado", constant = "false")
  EstudianteResponseDto toResponse(Estudiante model);
}
