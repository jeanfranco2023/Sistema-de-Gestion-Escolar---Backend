package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatriculaPersistenceMapper {
  Matricula toDomain(MatriculaEntity entity);

  MatriculaEntity toEntity(Matricula model);

  void actualizar(Matricula model, @org.mapstruct.MappingTarget MatriculaEntity entity);
}
