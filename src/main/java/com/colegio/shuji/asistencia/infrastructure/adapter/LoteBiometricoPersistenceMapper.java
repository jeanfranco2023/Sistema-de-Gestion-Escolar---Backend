package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.infrastructure.entity.LoteBiometricoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoteBiometricoPersistenceMapper {
  LoteBiometrico toDomain(LoteBiometricoEntity entity);

  LoteBiometricoEntity toEntity(LoteBiometrico model);

  void actualizar(LoteBiometrico model, @org.mapstruct.MappingTarget LoteBiometricoEntity entity);
}
