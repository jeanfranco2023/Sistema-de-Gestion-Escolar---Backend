package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaEstudianteApoderadoRepository
    extends JpaRepository<EstudianteApoderadoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EstudianteApoderadoEntity e where e.id = :id")
  Optional<EstudianteApoderadoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from EstudianteApoderadoEntity e where e.estudianteId = :valor order by e.id")
  List<EstudianteApoderadoEntity> buscarPorEstudianteId(@Param("valor") Long valor);

  @Query("select e from EstudianteApoderadoEntity e where e.apoderadoId = :valor order by e.id")
  List<EstudianteApoderadoEntity> buscarPorApoderadoId(@Param("valor") Long valor);
}
