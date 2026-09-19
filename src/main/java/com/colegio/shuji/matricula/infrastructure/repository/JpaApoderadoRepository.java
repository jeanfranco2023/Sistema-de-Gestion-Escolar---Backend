package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaApoderadoRepository extends JpaRepository<ApoderadoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ApoderadoEntity e where e.id = :id")
  Optional<ApoderadoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ApoderadoEntity e where e.usuarioId = :valor order by e.id")
  List<ApoderadoEntity> buscarPorUsuarioId(@Param("valor") Long valor);

  @Query("select e from ApoderadoEntity e where e.numeroDocumento = :valor order by e.id")
  List<ApoderadoEntity> buscarPorNumeroDocumento(@Param("valor") String valor);
}
