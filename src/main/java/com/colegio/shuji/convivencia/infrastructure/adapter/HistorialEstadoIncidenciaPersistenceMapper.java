package com.colegio.shuji.convivencia.infrastructure.adapter;

import com.colegio.shuji.convivencia.domain.model.CambioEstadoIncidencia;
import com.colegio.shuji.convivencia.infrastructure.entity.HistorialEstadoIncidenciaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HistorialEstadoIncidenciaPersistenceMapper {
  @Mapping(target = "nombreUsuario", source = "nombreUsuarioSnapshot")
  CambioEstadoIncidencia toDomain(HistorialEstadoIncidenciaEntity entity);

  @Mapping(target = "nombreUsuarioSnapshot", source = "nombreUsuario")
  HistorialEstadoIncidenciaEntity toEntity(CambioEstadoIncidencia model);
}
