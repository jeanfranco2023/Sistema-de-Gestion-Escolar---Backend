package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaApoderadoRepository extends JpaRepository<ApoderadoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ApoderadoEntity e where e.id = :id")
  Optional<ApoderadoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ApoderadoEntity e where e.usuarioId = :valor order by e.id")
  List<ApoderadoEntity> buscarPorUsuarioId(@Param("valor") Long valor);

  @Query("select e from ApoderadoEntity e where e.numeroDocumento = :valor order by e.id")
  List<ApoderadoEntity> buscarPorNumeroDocumento(@Param("valor") String valor);

  @Query("select e from ApoderadoEntity e where e.id in :ids order by e.id")
  List<ApoderadoEntity> buscarPorIds(@Param("ids") Collection<Long> ids);
}
