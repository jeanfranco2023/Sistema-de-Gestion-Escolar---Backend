package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaEstudianteRepository extends JpaRepository<EstudianteEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EstudianteEntity e where e.id = :id")
  Optional<EstudianteEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from EstudianteEntity e where e.numeroDocumento = :valor order by e.id")
  List<EstudianteEntity> buscarPorNumeroDocumento(@Param("valor") String valor);
}
