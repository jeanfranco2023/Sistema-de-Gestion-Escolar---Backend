package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.evaluacion.infrastructure.entity.SesionRefuerzoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SesionRefuerzoPersistenceMapper {
  SesionRefuerzo toDomain(SesionRefuerzoEntity entity);

  SesionRefuerzoEntity toEntity(SesionRefuerzo model);

  void actualizar(SesionRefuerzo model, @org.mapstruct.MappingTarget SesionRefuerzoEntity entity);
}
