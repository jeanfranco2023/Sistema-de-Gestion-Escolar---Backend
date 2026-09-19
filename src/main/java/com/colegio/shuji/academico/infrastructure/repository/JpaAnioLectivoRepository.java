package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAnioLectivoRepository extends JpaRepository<AnioLectivoEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AnioLectivoEntity e where e.id = :id")
  Optional<AnioLectivoEntity> bloquearPorId(@Param("id") Short id);
}
