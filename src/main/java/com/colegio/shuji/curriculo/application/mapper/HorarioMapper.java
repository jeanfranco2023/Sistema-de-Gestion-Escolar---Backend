package com.colegio.shuji.curriculo.application.mapper;

import com.colegio.shuji.curriculo.application.dto.out.HorarioSeccionResponseDto;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HorarioMapper {
  HorarioSeccionResponseDto toResponse(HorarioSeccion model);
}
