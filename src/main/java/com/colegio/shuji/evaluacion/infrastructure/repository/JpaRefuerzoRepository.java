package com.colegio.shuji.evaluacion.infrastructure.repository;

import com.colegio.shuji.evaluacion.infrastructure.entity.SesionRefuerzoEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRefuerzoRepository extends JpaRepository<SesionRefuerzoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SesionRefuerzoEntity e where e.id = :id")
  Optional<SesionRefuerzoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from SesionRefuerzoEntity e where e.anioLectivoId = :valor order by e.id")
  List<SesionRefuerzoEntity> buscarPorAnioLectivoId(@Param("valor") Short valor);

  @Query("select e from SesionRefuerzoEntity e where e.periodoAcademicoId = :valor order by e.id")
  List<SesionRefuerzoEntity> buscarPorPeriodoAcademicoId(@Param("valor") Short valor);

  @Query("select e from SesionRefuerzoEntity e where e.areaCurricularId = :valor order by e.id")
  List<SesionRefuerzoEntity> buscarPorAreaCurricularId(@Param("valor") Short valor);

  @Query("select e from SesionRefuerzoEntity e where e.docenteUsuarioId = :valor order by e.id")
  List<SesionRefuerzoEntity> buscarPorDocenteUsuarioId(@Param("valor") Long valor);
}
