package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.infrastructure.entity.MarcaBiometricoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaMarcaPorteriaRepository extends JpaRepository<MarcaBiometricoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MarcaBiometricoEntity e where e.id = :id")
  Optional<MarcaBiometricoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from MarcaBiometricoEntity e where e.loteId = :valor order by e.id")
  List<MarcaBiometricoEntity> buscarPorLoteId(@Param("valor") Long valor);

  @Query("select e from MarcaBiometricoEntity e where e.estudianteId = :valor order by e.id")
  List<MarcaBiometricoEntity> buscarPorEstudianteId(@Param("valor") Long valor);

  @Query("select e from MarcaBiometricoEntity e where e.fechaHora >= :inicio and e.fechaHora < :fin order by e.id")
  List<MarcaBiometricoEntity> buscarPorRangoFecha(
      @Param("inicio") java.time.OffsetDateTime inicio, @Param("fin") java.time.OffsetDateTime fin);
}
