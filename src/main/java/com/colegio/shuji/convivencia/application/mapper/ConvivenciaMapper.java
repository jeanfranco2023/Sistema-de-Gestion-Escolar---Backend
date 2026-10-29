package com.colegio.shuji.convivencia.application.mapper;

import com.colegio.shuji.convivencia.application.dto.in.RegistrarIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConvivenciaMapper {
  IncidenciaConductual toDomain(RegistrarIncidenciaRequestDto request);

  @Mapping(target = "nombreEstudiante", ignore = true)
  IncidenciaResponseDto toResponse(IncidenciaConductual model);
}
