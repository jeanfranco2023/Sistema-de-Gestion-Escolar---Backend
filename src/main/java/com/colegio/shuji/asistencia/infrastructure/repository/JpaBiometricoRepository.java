package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.infrastructure.entity.LoteBiometricoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaBiometricoRepository extends JpaRepository<LoteBiometricoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from LoteBiometricoEntity e where e.id = :id")
  Optional<LoteBiometricoEntity> bloquearPorId(@Param("id") Long id);

  @Query(
      "select e from LoteBiometricoEntity e where e.importadoPorUsuarioId = :valor order by e.id")
  List<LoteBiometricoEntity> buscarPorImportadoPorUsuarioId(@Param("valor") Long valor);
}
