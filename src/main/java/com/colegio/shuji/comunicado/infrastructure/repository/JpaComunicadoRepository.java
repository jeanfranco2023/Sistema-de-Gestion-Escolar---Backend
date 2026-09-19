package com.colegio.shuji.comunicado.infrastructure.repository;

import com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoOficialEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaComunicadoRepository extends JpaRepository<ComunicadoOficialEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ComunicadoOficialEntity e where e.id = :id")
  Optional<ComunicadoOficialEntity> bloquearPorId(@Param("id") Long id);

  @Query(
      "select e from ComunicadoOficialEntity e where e.remitenteUsuarioId = :valor order by e.id")
  List<ComunicadoOficialEntity> buscarPorRemitenteUsuarioId(@Param("valor") Long valor);

  @Query("select e from ComunicadoOficialEntity e where e.id in :ids order by e.id desc")
  List<ComunicadoOficialEntity> buscarPorIds(@Param("ids") Collection<Long> ids);
}
