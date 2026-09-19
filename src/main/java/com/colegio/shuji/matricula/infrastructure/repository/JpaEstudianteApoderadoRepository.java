package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

  @Query("select e from EstudianteApoderadoEntity e where e.estudianteId in :ids order by e.id")
  List<EstudianteApoderadoEntity> buscarPorEstudianteIds(@Param("ids") Collection<Long> ids);
}
