package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.domain.enums.*;
import com.colegio.shuji.asistencia.infrastructure.entity.AsistenciaAulaEntity;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaAsistenciaAulaRepository extends JpaRepository<AsistenciaAulaEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AsistenciaAulaEntity e where e.id = :id")
  Optional<AsistenciaAulaEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from AsistenciaAulaEntity e where e.matriculaId = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorMatriculaId(@Param("valor") Long valor);

  @Query("select e from AsistenciaAulaEntity e where e.fechaSesion = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorFechaSesion(@Param("valor") LocalDate valor);

  @Query("select e from AsistenciaAulaEntity e where e.estado = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorEstado(@Param("valor") EstadoAsistenciaAula valor);

  @Query("select e from AsistenciaAulaEntity e where e.auxiliarUsuarioId = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorAuxiliarUsuarioId(@Param("valor") Long valor);
}
