package com.colegio.shuji.academico.infrastructure.repository;

import com.colegio.shuji.academico.infrastructure.entity.AulaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAulaRepository extends JpaRepository<AulaEntity, Integer> {
  Optional<AulaEntity> findByCodigoIgnoreCase(String codigo);
}
