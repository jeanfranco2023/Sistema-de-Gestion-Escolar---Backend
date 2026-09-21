package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApoderadoPersistenceMapper {
  Apoderado toDomain(ApoderadoEntity entity);

  ApoderadoEntity toEntity(Apoderado model);

  void actualizar(Apoderado model, @org.mapstruct.MappingTarget ApoderadoEntity entity);
}
