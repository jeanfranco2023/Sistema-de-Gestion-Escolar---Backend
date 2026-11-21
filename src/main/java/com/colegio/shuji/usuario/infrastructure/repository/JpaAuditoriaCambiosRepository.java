package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.AuditoriaCambiosEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaAuditoriaCambiosRepository
    extends JpaRepository<AuditoriaCambiosEntity, Long>, JpaSpecificationExecutor<AuditoriaCambiosEntity> {

  List<AuditoriaCambiosEntity> findAllByOrderByFechaCreacionDescIdDesc(Pageable pageable);

}
