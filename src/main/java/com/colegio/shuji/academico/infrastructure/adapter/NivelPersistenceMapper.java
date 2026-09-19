package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.infrastructure.entity.NivelEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NivelPersistenceMapper {
  Nivel toDomain(NivelEntity entity);

  NivelEntity toEntity(Nivel model);

  void actualizar(Nivel model, @org.mapstruct.MappingTarget NivelEntity entity);
}
