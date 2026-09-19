package com.colegio.shuji.convivencia.infrastructure.repository;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.infrastructure.entity.IncidenciaConductualEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaIncidenciaRepository extends JpaRepository<IncidenciaConductualEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from IncidenciaConductualEntity e where e.id = :id")
  Optional<IncidenciaConductualEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from IncidenciaConductualEntity e where e.matriculaId = :valor order by e.id")
  List<IncidenciaConductualEntity> buscarPorMatriculaId(@Param("valor") Long valor);

  @Query(
      "select e from IncidenciaConductualEntity e where e.reportadoPorUsuarioId = :valor order by"
          + " e.id")
  List<IncidenciaConductualEntity> buscarPorReportadoPorUsuarioId(@Param("valor") Long valor);

  @Query("select e from IncidenciaConductualEntity e where e.estado = :valor order by e.id")
  List<IncidenciaConductualEntity> buscarPorEstado(@Param("valor") EstadoIncidencia valor);
}
