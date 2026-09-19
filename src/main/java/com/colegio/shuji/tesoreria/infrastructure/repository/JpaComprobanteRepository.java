package com.colegio.shuji.tesoreria.infrastructure.repository;

import com.colegio.shuji.tesoreria.infrastructure.entity.ComprobanteEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaComprobanteRepository extends JpaRepository<ComprobanteEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ComprobanteEntity e where e.id = :id")
  Optional<ComprobanteEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ComprobanteEntity e where e.pagoTransaccionId = :valor order by e.id")
  List<ComprobanteEntity> buscarPorPagoTransaccionId(@Param("valor") Long valor);

  Optional<ComprobanteEntity> findFirstBySerieOrderByCorrelativoDesc(String serie);
}
