package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEstudianteRepository extends JpaRepository<EstudianteEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EstudianteEntity e where e.id = :id")
  Optional<EstudianteEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from EstudianteEntity e where e.numeroDocumento = :valor order by e.id")
  List<EstudianteEntity> buscarPorNumeroDocumento(@Param("valor") String valor);

  @Query("select e from EstudianteEntity e where e.numeroDocumento in :numeros order by e.id")
  List<EstudianteEntity> buscarPorNumerosDocumento(@Param("numeros") Collection<String> numeros);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EstudianteEntity e where e.id in :ids order by e.id")
  List<EstudianteEntity> bloquearPorIds(@Param("ids") Collection<Long> ids);
}
