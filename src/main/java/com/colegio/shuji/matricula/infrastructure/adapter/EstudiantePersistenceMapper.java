package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EstudiantePersistenceMapper {
  Estudiante toDomain(EstudianteEntity entity);

  EstudianteEntity toEntity(Estudiante model);

  void actualizar(Estudiante model, @org.mapstruct.MappingTarget EstudianteEntity entity);
}
