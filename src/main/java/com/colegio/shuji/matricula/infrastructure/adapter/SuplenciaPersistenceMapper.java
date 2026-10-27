package com.colegio.shuji.matricula.infrastructure.adapter;
import com.colegio.shuji.matricula.domain.model.Suplencia;
import com.colegio.shuji.matricula.infrastructure.entity.SuplenciaEntity;
import org.mapstruct.Mapper;
@Mapper(componentModel="spring")
public interface SuplenciaPersistenceMapper {
  SuplenciaEntity toEntity(Suplencia valor);
  Suplencia toDomain(SuplenciaEntity valor);
}
