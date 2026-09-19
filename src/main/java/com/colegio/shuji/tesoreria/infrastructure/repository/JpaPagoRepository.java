package com.colegio.shuji.tesoreria.infrastructure.repository;

import com.colegio.shuji.tesoreria.infrastructure.entity.PagoTransaccionEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaPagoRepository extends JpaRepository<PagoTransaccionEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from PagoTransaccionEntity e where e.id = :id")
  Optional<PagoTransaccionEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from PagoTransaccionEntity e where e.obligacionPagoId = :valor order by e.id")
  List<PagoTransaccionEntity> buscarPorObligacionPagoId(@Param("valor") Long valor);

  @Query(
      "select e from PagoTransaccionEntity e where e.pasarelaTransaccionId = :valor order by e.id")
  List<PagoTransaccionEntity> buscarPorPasarelaTransaccionId(@Param("valor") String valor);
}
