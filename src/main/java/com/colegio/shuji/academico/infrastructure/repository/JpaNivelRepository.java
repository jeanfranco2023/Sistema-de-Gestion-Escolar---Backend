package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.domain.enums.*;
import com.colegio.shuji.academico.infrastructure.entity.NivelEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaNivelRepository extends JpaRepository<NivelEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from NivelEntity e where e.id = :id")
  Optional<NivelEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from NivelEntity e where e.codigo = :valor order by e.id")
  List<NivelEntity> buscarPorCodigo(@Param("valor") NivelCodigo valor);
}
