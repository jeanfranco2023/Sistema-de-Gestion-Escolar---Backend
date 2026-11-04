package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.AuditoriaCambiosEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditoriaCambiosRepository
    extends JpaRepository<AuditoriaCambiosEntity, Long> {

  List<AuditoriaCambiosEntity> findAllByOrderByFechaCreacionDescIdDesc(Pageable pageable);
}
