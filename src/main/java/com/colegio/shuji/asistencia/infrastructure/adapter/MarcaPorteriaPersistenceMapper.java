package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import com.colegio.shuji.asistencia.infrastructure.entity.MarcaBiometricoEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarcaPorteriaPersistenceMapper {
  MarcaPorteria toDomain(MarcaBiometricoEntity entity);

  MarcaBiometricoEntity toEntity(MarcaPorteria model);

  void actualizar(MarcaPorteria model, @org.mapstruct.MappingTarget MarcaBiometricoEntity entity);
}
