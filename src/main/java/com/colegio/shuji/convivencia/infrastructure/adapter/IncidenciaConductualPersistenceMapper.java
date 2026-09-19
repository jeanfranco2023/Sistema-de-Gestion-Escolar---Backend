package com.colegio.shuji.convivencia.infrastructure.adapter;

import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.convivencia.infrastructure.entity.IncidenciaConductualEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IncidenciaConductualPersistenceMapper {
  IncidenciaConductual toDomain(IncidenciaConductualEntity entity);

  IncidenciaConductualEntity toEntity(IncidenciaConductual model);

  void actualizar(
      IncidenciaConductual model, @org.mapstruct.MappingTarget IncidenciaConductualEntity entity);
}
