package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.curriculo.infrastructure.entity.CompetenciaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompetenciaPersistenceMapper {
  Competencia toDomain(CompetenciaEntity entity);

  CompetenciaEntity toEntity(Competencia model);

  void actualizar(Competencia model, @org.mapstruct.MappingTarget CompetenciaEntity entity);
}
