package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.infrastructure.entity.GradoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GradoPersistenceMapper {
  Grado toDomain(GradoEntity entity);

  GradoEntity toEntity(Grado model);

  void actualizar(Grado model, @org.mapstruct.MappingTarget GradoEntity entity);
}
