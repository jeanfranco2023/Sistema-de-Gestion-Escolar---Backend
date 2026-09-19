package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.AsignacionDocenteEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAsignacionRepository extends JpaRepository<AsignacionDocenteEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AsignacionDocenteEntity e where e.id = :id")
  Optional<AsignacionDocenteEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from AsignacionDocenteEntity e where e.docenteUsuarioId = :valor order by e.id")
  List<AsignacionDocenteEntity> buscarPorDocenteUsuarioId(@Param("valor") Long valor);

  @Query("select e from AsignacionDocenteEntity e where e.seccionId = :valor order by e.id")
  List<AsignacionDocenteEntity> buscarPorSeccionId(@Param("valor") Integer valor);

  @Query("select e from AsignacionDocenteEntity e where e.anioLectivoId = :valor order by e.id")
  List<AsignacionDocenteEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from AsignacionDocenteEntity e where e.nivelId = :valor order by e.id")
  List<AsignacionDocenteEntity> buscarPorNivelId(@Param("valor") Short valor);

  @Query("select e from AsignacionDocenteEntity e where e.areaCurricularId = :valor order by e.id")
  List<AsignacionDocenteEntity> buscarPorAreaCurricularId(@Param("valor") Short valor);
}
