package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.CompetenciaEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCompetenciaRepository extends JpaRepository<CompetenciaEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CompetenciaEntity e where e.id = :id")
  Optional<CompetenciaEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from CompetenciaEntity e where e.areaId = :valor order by e.id")
  List<CompetenciaEntity> buscarPorAreaId(@Param("valor") Short valor);

  @Query("select e from CompetenciaEntity e where e.id in :ids order by e.id")
  List<CompetenciaEntity> buscarPorIds(@Param("ids") Collection<Short> ids);
}
