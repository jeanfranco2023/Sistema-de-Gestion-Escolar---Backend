package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.asistencia.infrastructure.entity.AsistenciaAulaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AsistenciaAulaPersistenceMapper {
  AsistenciaAula toDomain(AsistenciaAulaEntity entity);

  AsistenciaAulaEntity toEntity(AsistenciaAula model);

  void actualizar(AsistenciaAula model, @org.mapstruct.MappingTarget AsistenciaAulaEntity entity);
}
