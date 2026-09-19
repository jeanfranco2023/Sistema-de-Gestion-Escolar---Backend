package com.colegio.shuji.comunicado.infrastructure.adapter;

import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoDestinatarioEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComunicadoDestinatarioPersistenceMapper {
  ComunicadoDestinatario toDomain(ComunicadoDestinatarioEntity entity);

  ComunicadoDestinatarioEntity toEntity(ComunicadoDestinatario model);

  void actualizar(
      ComunicadoDestinatario model,
      @org.mapstruct.MappingTarget ComunicadoDestinatarioEntity entity);
}
