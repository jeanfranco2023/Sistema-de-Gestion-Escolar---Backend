package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.infrastructure.entity.BloqueHorarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BloqueHorarioPersistenceMapper {
  BloqueHorario toDomain(BloqueHorarioEntity entity);

  BloqueHorarioEntity toEntity(BloqueHorario model);

  void actualizar(BloqueHorario model, @org.mapstruct.MappingTarget BloqueHorarioEntity entity);
}
