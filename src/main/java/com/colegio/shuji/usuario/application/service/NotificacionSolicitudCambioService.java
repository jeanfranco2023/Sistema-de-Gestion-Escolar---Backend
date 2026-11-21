package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.usuario.application.port.out.SolicitudCambioCorreoPort;
import com.colegio.shuji.usuario.application.port.out.SolicitudCambioRepositoryPort;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.model.EventoSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.NotificacionSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.SolicitudCambio;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
class NotificacionSolicitudCambioService {

  private final SolicitudCambioRepositoryPort repository;
  private final SolicitudCambioCorreoPort correo;
  private final ApplicationEventPublisher eventos;

  void encolar(
      SolicitudCambio solicitud, EventoSolicitudCambio evento, String asunto, String cuerpo) {
    NotificacionSolicitudCambio notificacion =
        repository.saveNotificacion(
            NotificacionSolicitudCambio.builder()
                .solicitudId(solicitud.getId())
                .eventoId(evento.getId())
                .destinatarioEmail(solicitud.getSolicitanteEmail())
                .asunto(asunto)
                .cuerpoResumen(cuerpo)
                .estadoEnvio("PENDIENTE")
                .intentos(0)
                .claveIdempotencia(solicitud.getCodigo() + ":ev-" + evento.getId())
                .fechaIntento(evento.getFechaEvento())
                .build());
    eventos.publishEvent(new NotificacionCreada(solicitud.getId(), notificacion.getId()));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void entregarConfirmada(NotificacionCreada evento) {
    entregar(evento.solicitudId(), evento.notificacionId());
  }

  @Transactional
  public NotificacionSolicitudCambio entregar(Long solicitudId, Long notificacionId) {
    SolicitudCambio solicitud =
        repository
            .findById(solicitudId)
            .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cambio no encontrada."));
    NotificacionSolicitudCambio notificacion =
        repository
            .bloquearNotificacionPorId(notificacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
    if (!solicitudId.equals(notificacion.getSolicitudId())) {
      throw new ReglaNegocioException(
          "La notificación no pertenece a la solicitud de cambio indicada.",
          "NOTIFICACION_INCONSISTENTE");
    }
    if ("ENVIADO".equals(notificacion.getEstadoEnvio())) {
      return notificacion;
    }

    var evento =
        notificacion.getEventoId() == null
            ? java.util.Optional.<EventoSolicitudCambio>empty()
            : repository.findEventoById(notificacion.getEventoId());
    String estadoNotificado =
        evento.map(EventoSolicitudCambio::getEstadoNuevo)
            .orElseGet(() -> solicitud.getEstado().name());
    String cuerpoPublico =
        evento.filter(e -> Boolean.TRUE.equals(e.getVisibleDireccion()))
            .map(e -> notificacion.getCuerpoResumen())
            .orElse("");
    OffsetDateTime ahora = OffsetDateTime.now();
    try {
      correo.enviarNotificacionSolicitudCambio(
          notificacion.getDestinatarioEmail(),
          solicitud.getCodigo(),
          solicitud.getTitulo(),
          estadoNotificado,
          notificacion.getAsunto(),
          cuerpoPublico);
      notificacion.registrarResultadoEnvio(true, null, ahora);
    } catch (RuntimeException ex) {
      log.warn("Error al entregar notificación RFC {}: {}", notificacionId, ex.getClass().getSimpleName());
      notificacion.registrarResultadoEnvio(
          false, "No se pudo enviar el correo. " + ex.getClass().getSimpleName(), ahora);
    }
    return repository.saveNotificacion(notificacion);
  }

  record NotificacionCreada(Long solicitudId, Long notificacionId) {}
}
