package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaSeccionRepository extends JpaRepository<SeccionEntity, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SeccionEntity e where e.id = :id")
  Optional<SeccionEntity> bloquearPorId(@Param("id") Integer id);

  @Query("select e from SeccionEntity e where e.anioLectivoId = :valor order by e.id")
  List<SeccionEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from SeccionEntity e where e.gradoId = :valor order by e.id")
  List<SeccionEntity> buscarPorGradoId(@Param("valor") Short valor);

  @Query("select e from SeccionEntity e where e.nivelId = :valor order by e.id")
  List<SeccionEntity> buscarPorNivelId(@Param("valor") Short valor);
}
