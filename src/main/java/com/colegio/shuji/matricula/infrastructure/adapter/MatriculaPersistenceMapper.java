package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatriculaPersistenceMapper {
  Matricula toDomain(MatriculaEntity entity);

  MatriculaEntity toEntity(Matricula model);

  void actualizar(Matricula model, @org.mapstruct.MappingTarget MatriculaEntity entity);
}
