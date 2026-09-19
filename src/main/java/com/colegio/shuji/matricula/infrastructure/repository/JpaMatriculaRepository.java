package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.domain.enums.*;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaMatriculaRepository extends JpaRepository<MatriculaEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MatriculaEntity e where e.id = :id")
  Optional<MatriculaEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from MatriculaEntity e where e.anioLectivoId = :valor order by e.id")
  List<MatriculaEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from MatriculaEntity e where e.estudianteId = :valor order by e.id")
  List<MatriculaEntity> buscarPorEstudianteId(@Param("valor") Long valor);

  @Query("select e from MatriculaEntity e where e.seccionId = :valor order by e.id")
  List<MatriculaEntity> buscarPorSeccionId(@Param("valor") Integer valor);

  @Query("select e from MatriculaEntity e where e.estadoMatricula = :valor order by e.id")
  List<MatriculaEntity> buscarPorEstadoMatricula(@Param("valor") EstadoMatricula valor);
}
