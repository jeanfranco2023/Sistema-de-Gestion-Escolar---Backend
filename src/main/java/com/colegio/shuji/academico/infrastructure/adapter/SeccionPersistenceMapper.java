package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SeccionPersistenceMapper {
  Seccion toDomain(SeccionEntity entity);

  SeccionEntity toEntity(Seccion model);

  void actualizar(Seccion model, @org.mapstruct.MappingTarget SeccionEntity entity);
}
