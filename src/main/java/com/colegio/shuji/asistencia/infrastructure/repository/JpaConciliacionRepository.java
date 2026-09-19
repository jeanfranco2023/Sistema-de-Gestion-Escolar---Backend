package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.infrastructure.entity.ConciliacionAsistenciaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaConciliacionRepository
    extends JpaRepository<ConciliacionAsistenciaEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ConciliacionAsistenciaEntity e where e.id = :id")
  Optional<ConciliacionAsistenciaEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ConciliacionAsistenciaEntity e where e.fecha = :valor order by e.id")
  List<ConciliacionAsistenciaEntity> buscarPorFecha(@Param("valor") LocalDate valor);

  @Query("select e from ConciliacionAsistenciaEntity e where e.estudianteId = :valor order by e.id")
  List<ConciliacionAsistenciaEntity> buscarPorEstudianteId(@Param("valor") Long valor);
}
