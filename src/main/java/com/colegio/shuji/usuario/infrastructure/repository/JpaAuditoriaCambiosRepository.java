package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.AuditoriaCambiosEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAuditoriaCambiosRepository
    extends JpaRepository<AuditoriaCambiosEntity, Long> {

  List<AuditoriaCambiosEntity> findAllByOrderByFechaCreacionDescIdDesc(Pageable pageable);

  @Query("SELECT a FROM AuditoriaCambiosEntity a WHERE "
      + "(:fechaInicio IS NULL OR a.fechaCreacion >= :fechaInicio) AND "
      + "(:fechaFin IS NULL OR a.fechaCreacion <= :fechaFin) AND "
      + "(:usuarioId IS NULL OR a.usuario.id = :usuarioId) AND "
      + "(:tabla IS NULL OR LOWER(a.tablaAfectada) = LOWER(:tabla)) "
      + "ORDER BY a.fechaCreacion DESC, a.id DESC")
  Page<AuditoriaCambiosEntity> filtrar(
      @Param("fechaInicio") Instant fechaInicio,
      @Param("fechaFin") Instant fechaFin,
      @Param("usuarioId") Long usuarioId,
      @Param("tabla") String tabla,
      Pageable pageable);
}
