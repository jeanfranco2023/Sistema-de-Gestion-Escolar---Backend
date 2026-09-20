package com.colegio.shuji.comunicado.infrastructure.adapter;

import com.colegio.shuji.comunicado.domain.model.ComunicadoOficial;
import com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoOficialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComunicadoOficialPersistenceMapper {
  ComunicadoOficial toDomain(ComunicadoOficialEntity entity);

  ComunicadoOficialEntity toEntity(ComunicadoOficial model);

  void actualizar(
      ComunicadoOficial model, @org.mapstruct.MappingTarget ComunicadoOficialEntity entity);
}
