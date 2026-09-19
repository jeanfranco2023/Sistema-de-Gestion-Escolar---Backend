package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.HorarioSeccionEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaHorarioRepository extends JpaRepository<HorarioSeccionEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from HorarioSeccionEntity e where e.id = :id")
  Optional<HorarioSeccionEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from HorarioSeccionEntity e where e.anioLectivoId = :valor order by e.id")
  List<HorarioSeccionEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from HorarioSeccionEntity e where e.seccionId = :valor order by e.id")
  List<HorarioSeccionEntity> buscarPorSeccionId(@Param("valor") Integer valor);

  @Query("select e from HorarioSeccionEntity e where e.docenteUsuarioId = :valor order by e.id")
  List<HorarioSeccionEntity> buscarPorDocenteUsuarioId(@Param("valor") Long valor);

  @Query("select e from HorarioSeccionEntity e where e.asignacionDocenteId = :valor order by e.id")
  List<HorarioSeccionEntity> buscarPorAsignacionDocenteId(@Param("valor") Long valor);

  @Query("select e from HorarioSeccionEntity e where e.bloqueHorarioId = :valor order by e.id")
  List<HorarioSeccionEntity> buscarPorBloqueHorarioId(@Param("valor") Short valor);
}
