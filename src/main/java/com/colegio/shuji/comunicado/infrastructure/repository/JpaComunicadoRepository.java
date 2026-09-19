package com.colegio.shuji.comunicado.infrastructure.repository;

import com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoOficialEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaComunicadoRepository extends JpaRepository<ComunicadoOficialEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ComunicadoOficialEntity e where e.id = :id")
  Optional<ComunicadoOficialEntity> bloquearPorId(@Param("id") Long id);

  @Query(
      "select e from ComunicadoOficialEntity e where e.remitenteUsuarioId = :valor order by e.id")
  List<ComunicadoOficialEntity> buscarPorRemitenteUsuarioId(@Param("valor") Long valor);
}
