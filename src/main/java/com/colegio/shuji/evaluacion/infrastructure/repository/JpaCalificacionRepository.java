package com.colegio.shuji.evaluacion.infrastructure.repository;

import com.colegio.shuji.evaluacion.infrastructure.entity.CalificacionCnebEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCalificacionRepository extends JpaRepository<CalificacionCnebEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CalificacionCnebEntity e where e.id = :id")
  Optional<CalificacionCnebEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from CalificacionCnebEntity e where e.matriculaId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorMatriculaId(@Param("valor") Long valor);

  @Query("select e from CalificacionCnebEntity e where e.anioLectivoId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from CalificacionCnebEntity e where e.seccionId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorSeccionId(@Param("valor") Integer valor);

  @Query("select e from CalificacionCnebEntity e where e.periodoAcademicoId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorPeriodoAcademicoId(@Param("valor") Short valor);

  @Query(
      "select e from CalificacionCnebEntity e where e.asignacionDocenteId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorAsignacionDocenteId(@Param("valor") Long valor);

  @Query("select e from CalificacionCnebEntity e where e.areaCurricularId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorAreaCurricularId(@Param("valor") Short valor);

  @Query("select e from CalificacionCnebEntity e where e.docenteUsuarioId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorDocenteUsuarioId(@Param("valor") Long valor);

  @Query("select e from CalificacionCnebEntity e where e.competenciaId = :valor order by e.id")
  List<CalificacionCnebEntity> buscarPorCompetenciaId(@Param("valor") Short valor);

  @Query("select e from CalificacionCnebEntity e where e.asignacionDocenteId = :asigId and e.periodoAcademicoId = :periodoId order by e.id")
  List<CalificacionCnebEntity> buscarPorAsignacionYPeriodo(@Param("asigId") Long asigId, @Param("periodoId") Short periodoId);
}
