package com.colegio.shuji.matricula.application.mapper;

import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApoderadoMapper {
  ApoderadoResponseDto toResponse(Apoderado model);
}
