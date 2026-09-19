package com.colegio.shuji.matricula.application.mapper;

import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstudianteMapper {
  EstudianteResponseDto toResponse(Estudiante model);
}
