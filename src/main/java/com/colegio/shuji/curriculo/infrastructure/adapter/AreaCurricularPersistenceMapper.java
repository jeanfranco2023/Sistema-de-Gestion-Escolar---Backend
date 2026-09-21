package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.infrastructure.entity.AreaCurricularEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AreaCurricularPersistenceMapper {
  AreaCurricular toDomain(AreaCurricularEntity entity);

  AreaCurricularEntity toEntity(AreaCurricular model);

  void actualizar(AreaCurricular model, @org.mapstruct.MappingTarget AreaCurricularEntity entity);
}
