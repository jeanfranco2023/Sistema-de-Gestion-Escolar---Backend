package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.infrastructure.entity.CalificacionCnebEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CalificacionCnebPersistenceMapper {
  CalificacionCneb toDomain(CalificacionCnebEntity entity);

  CalificacionCnebEntity toEntity(CalificacionCneb model);

  void actualizar(
      CalificacionCneb model, @org.mapstruct.MappingTarget CalificacionCnebEntity entity);
}
