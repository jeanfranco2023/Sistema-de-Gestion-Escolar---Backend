package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.infrastructure.entity.ConceptoCobroEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConceptoCobroPersistenceMapper {
  ConceptoCobro toDomain(ConceptoCobroEntity entity);

  ConceptoCobroEntity toEntity(ConceptoCobro model);

  void actualizar(ConceptoCobro model, @org.mapstruct.MappingTarget ConceptoCobroEntity entity);
}
