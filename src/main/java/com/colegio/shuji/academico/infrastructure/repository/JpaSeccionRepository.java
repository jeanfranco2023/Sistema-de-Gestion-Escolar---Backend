package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSeccionRepository extends JpaRepository<SeccionEntity, Integer> {
  @Modifying
  @Query(value = "update secciones set vacantes_ocupadas = vacantes_ocupadas + 1 where id = :id and vacantes_ocupadas < cupo_maximo", nativeQuery = true)
  int reservarVacante(@Param("id") Integer id);

  @Modifying
  @Query(value = "update secciones set vacantes_ocupadas = vacantes_ocupadas - 1 where id = :id and vacantes_ocupadas > 0", nativeQuery = true)
  int liberarVacante(@Param("id") Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SeccionEntity e where e.id = :id")
  Optional<SeccionEntity> bloquearPorId(@Param("id") Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SeccionEntity e where e.id in :ids order by e.id")
  List<SeccionEntity> bloquearPorIds(@Param("ids") Collection<Integer> ids);

  @Query("select e from SeccionEntity e where e.anioLectivoId = :valor order by e.id")
  List<SeccionEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from SeccionEntity e where e.gradoId = :valor order by e.id")
  List<SeccionEntity> buscarPorGradoId(@Param("valor") Short valor);

  @Query("select e from SeccionEntity e where e.nivelId = :valor order by e.id")
  List<SeccionEntity> buscarPorNivelId(@Param("valor") Short valor);
}
