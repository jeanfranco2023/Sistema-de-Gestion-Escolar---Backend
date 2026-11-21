package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.EventoSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.NotificacionSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.SolicitudCambio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudCambioRepositoryPort {

  SolicitudCambio save(SolicitudCambio solicitud);

  Optional<SolicitudCambio> findById(Long id);

  Optional<SolicitudCambio> bloquearPorId(Long id);

  void bloquearSolicitante(Long solicitanteId);

  Optional<SolicitudCambio> findBySolicitanteIdAndIdempotencyKey(
      Long solicitanteId, String idempotencyKey);

  Optional<SolicitudCambio> findRecienteDuplicada(
      Long solicitanteId, String titulo, String modulo, OffsetDateTime desde);

  List<SolicitudCambio> findBySolicitanteIdOrderByFechaSolicitudDesc(Long solicitanteId);

  List<SolicitudCambio> findAllOrderByFechaSolicitudDesc();

  long siguienteFolio();

  EventoSolicitudCambio saveEvento(EventoSolicitudCambio evento);

  Optional<EventoSolicitudCambio> findEventoById(Long eventoId);

  List<EventoSolicitudCambio> findEventosBySolicitudId(Long solicitudId);

  NotificacionSolicitudCambio saveNotificacion(NotificacionSolicitudCambio notificacion);

  Optional<NotificacionSolicitudCambio> bloquearNotificacionPorId(Long notificacionId);

  List<NotificacionSolicitudCambio> findNotificacionesBySolicitudId(Long solicitudId);
}
