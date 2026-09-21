package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.infrastructure.entity.InscripcionRefuerzoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InscripcionRefuerzoPersistenceMapper {
  InscripcionRefuerzo toDomain(InscripcionRefuerzoEntity entity);

  InscripcionRefuerzoEntity toEntity(InscripcionRefuerzo model);

  void actualizar(
      InscripcionRefuerzo model, @org.mapstruct.MappingTarget InscripcionRefuerzoEntity entity);
}
