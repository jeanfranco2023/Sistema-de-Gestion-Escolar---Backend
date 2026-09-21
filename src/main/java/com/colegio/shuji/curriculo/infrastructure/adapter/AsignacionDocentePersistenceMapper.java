package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.infrastructure.entity.AsignacionDocenteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AsignacionDocentePersistenceMapper {
  AsignacionDocente toDomain(AsignacionDocenteEntity entity);

  AsignacionDocenteEntity toEntity(AsignacionDocente model);

  void actualizar(
      AsignacionDocente model, @org.mapstruct.MappingTarget AsignacionDocenteEntity entity);
}
