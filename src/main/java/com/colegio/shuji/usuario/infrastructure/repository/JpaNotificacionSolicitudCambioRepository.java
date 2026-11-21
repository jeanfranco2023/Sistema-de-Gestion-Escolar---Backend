package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.NotificacionSolicitudCambioEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaNotificacionSolicitudCambioRepository
    extends JpaRepository<NotificacionSolicitudCambioEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT n FROM NotificacionSolicitudCambioEntity n WHERE n.id = :id")
  Optional<NotificacionSolicitudCambioEntity> bloquearPorId(@Param("id") Long id);

  List<NotificacionSolicitudCambioEntity> findBySolicitudIdOrderByFechaIntentoDescIdDesc(
      Long solicitudId);
}
