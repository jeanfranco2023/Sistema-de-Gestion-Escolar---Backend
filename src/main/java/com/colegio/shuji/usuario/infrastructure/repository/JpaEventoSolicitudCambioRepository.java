package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.EventoSolicitudCambioEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventoSolicitudCambioRepository
    extends JpaRepository<EventoSolicitudCambioEntity, Long> {

  List<EventoSolicitudCambioEntity> findBySolicitudIdOrderByFechaEventoAscIdAsc(Long solicitudId);
}
