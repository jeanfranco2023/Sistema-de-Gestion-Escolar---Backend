package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.asistencia.infrastructure.entity.ConciliacionAsistenciaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConciliacionAsistenciaPersistenceMapper {
  ConciliacionAsistencia toDomain(ConciliacionAsistenciaEntity entity);

  ConciliacionAsistenciaEntity toEntity(ConciliacionAsistencia model);

  void actualizar(
      ConciliacionAsistencia model,
      @org.mapstruct.MappingTarget ConciliacionAsistenciaEntity entity);
}
