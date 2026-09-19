package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.GradoEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaGradoRepository extends JpaRepository<GradoEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from GradoEntity e where e.id = :id")
  Optional<GradoEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from GradoEntity e where e.nivelId = :valor order by e.id")
  List<GradoEntity> buscarPorNivelId(@Param("valor") Short valor);
}
