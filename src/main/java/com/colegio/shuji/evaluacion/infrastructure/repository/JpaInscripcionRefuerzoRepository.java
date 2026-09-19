package com.colegio.shuji.evaluacion.infrastructure.repository;

import com.colegio.shuji.evaluacion.infrastructure.entity.InscripcionRefuerzoEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInscripcionRefuerzoRepository
    extends JpaRepository<InscripcionRefuerzoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from InscripcionRefuerzoEntity e where e.id = :id")
  Optional<InscripcionRefuerzoEntity> bloquearPorId(@Param("id") Long id);

  @Query(
      "select e from InscripcionRefuerzoEntity e where e.sesionRefuerzoId = :valor order by e.id")
  List<InscripcionRefuerzoEntity> buscarPorSesionRefuerzoId(@Param("valor") Long valor);

  @Query("select e from InscripcionRefuerzoEntity e where e.estudianteId = :valor order by e.id")
  List<InscripcionRefuerzoEntity> buscarPorEstudianteId(@Param("valor") Long valor);

  @Query(
      "select e from InscripcionRefuerzoEntity e where e.calificacionOrigenId = :valor order by"
          + " e.id")
  List<InscripcionRefuerzoEntity> buscarPorCalificacionOrigenId(@Param("valor") Long valor);
}
