package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.curriculo.infrastructure.entity.HorarioSeccionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HorarioSeccionPersistenceMapper {
  HorarioSeccion toDomain(HorarioSeccionEntity entity);

  HorarioSeccionEntity toEntity(HorarioSeccion model);

  void actualizar(HorarioSeccion model, @org.mapstruct.MappingTarget HorarioSeccionEntity entity);
}
