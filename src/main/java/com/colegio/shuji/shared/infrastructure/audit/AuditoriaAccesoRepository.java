package com.colegio.shuji.shared.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditoriaAccesoRepository extends JpaRepository<AuditoriaAccesoEntity, Long> {
  Page<AuditoriaAccesoEntity> findAllByOrderByFechaEventoDesc(Pageable pageable);
}
