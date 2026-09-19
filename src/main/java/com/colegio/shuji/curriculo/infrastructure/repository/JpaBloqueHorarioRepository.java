package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.BloqueHorarioEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaBloqueHorarioRepository extends JpaRepository<BloqueHorarioEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from BloqueHorarioEntity e where e.id = :id")
  Optional<BloqueHorarioEntity> bloquearPorId(@Param("id") Short id);
}
