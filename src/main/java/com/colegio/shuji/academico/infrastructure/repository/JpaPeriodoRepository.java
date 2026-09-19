package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.PeriodoAcademicoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaPeriodoRepository extends JpaRepository<PeriodoAcademicoEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from PeriodoAcademicoEntity e where e.id = :id")
  Optional<PeriodoAcademicoEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from PeriodoAcademicoEntity e where e.anioLectivoId = :valor order by e.id")
  List<PeriodoAcademicoEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);
}
