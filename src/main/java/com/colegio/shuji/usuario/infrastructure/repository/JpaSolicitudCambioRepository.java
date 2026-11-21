package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.SolicitudCambioEntity;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSolicitudCambioRepository extends JpaRepository<SolicitudCambioEntity, Long> {

  Optional<SolicitudCambioEntity> findBySolicitanteIdAndIdempotencyKey(
      Long solicitanteId, String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM SolicitudCambioEntity s WHERE s.id = :id")
  Optional<SolicitudCambioEntity> bloquearPorId(@Param("id") Long id);

  @Query(value = "SELECT id FROM public.usuarios WHERE id = :id FOR NO KEY UPDATE", nativeQuery = true)
  Optional<Long> bloquearSolicitante(@Param("id") Long id);

  @Query("""
      SELECT s FROM SolicitudCambioEntity s
      WHERE s.solicitanteId = :solicitanteId
        AND LOWER(s.titulo) = LOWER(:titulo)
        AND LOWER(s.modulo) = LOWER(:modulo)
        AND s.fechaSolicitud >= :desde
      ORDER BY s.fechaSolicitud DESC
      """)
  List<SolicitudCambioEntity> findRecientesDuplicadas(
      @Param("solicitanteId") Long solicitanteId,
      @Param("titulo") String titulo,
      @Param("modulo") String modulo,
      @Param("desde") OffsetDateTime desde);

  List<SolicitudCambioEntity> findBySolicitanteIdOrderByFechaSolicitudDesc(Long solicitanteId);

  List<SolicitudCambioEntity> findAllByOrderByFechaSolicitudDesc();

  @Query(value = "SELECT nextval('public.solicitudes_cambio_folio_seq')", nativeQuery = true)
  long siguienteFolio();
}
