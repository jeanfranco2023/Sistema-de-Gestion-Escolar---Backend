package com.colegio.shuji.tesoreria.infrastructure.repository;

import com.colegio.shuji.tesoreria.domain.enums.*;
import com.colegio.shuji.tesoreria.infrastructure.entity.ObligacionPagoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaObligacionRepository extends JpaRepository<ObligacionPagoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ObligacionPagoEntity e where e.id = :id")
  Optional<ObligacionPagoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ObligacionPagoEntity e where e.matriculaId = :valor order by e.id")
  List<ObligacionPagoEntity> buscarPorMatriculaId(@Param("valor") Long valor);

  @Query("select e from ObligacionPagoEntity e where e.matriculaId in :ids order by e.id")
  List<ObligacionPagoEntity> buscarPorMatriculaIds(@Param("ids") Collection<Long> ids);

  @Query("select e from ObligacionPagoEntity e where e.conceptoId = :valor order by e.id")
  List<ObligacionPagoEntity> buscarPorConceptoId(@Param("valor") Short valor);

  @Query("select e from ObligacionPagoEntity e where e.estado = :valor order by e.id")
  List<ObligacionPagoEntity> buscarPorEstado(@Param("valor") EstadoObligacion valor);
}
