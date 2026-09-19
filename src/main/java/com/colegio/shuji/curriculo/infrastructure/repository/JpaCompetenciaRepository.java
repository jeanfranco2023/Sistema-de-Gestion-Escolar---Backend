package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.CompetenciaEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaCompetenciaRepository extends JpaRepository<CompetenciaEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CompetenciaEntity e where e.id = :id")
  Optional<CompetenciaEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from CompetenciaEntity e where e.areaId = :valor order by e.id")
  List<CompetenciaEntity> buscarPorAreaId(@Param("valor") Short valor);
}
