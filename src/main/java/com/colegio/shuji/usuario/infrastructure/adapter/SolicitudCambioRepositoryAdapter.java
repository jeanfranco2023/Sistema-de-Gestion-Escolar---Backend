package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.SolicitudCambioRepositoryPort;
import com.colegio.shuji.usuario.domain.model.EventoSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.NotificacionSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.SolicitudCambio;
import com.colegio.shuji.usuario.infrastructure.entity.EventoSolicitudCambioEntity;
import com.colegio.shuji.usuario.infrastructure.entity.NotificacionSolicitudCambioEntity;
import com.colegio.shuji.usuario.infrastructure.entity.SolicitudCambioEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaEventoSolicitudCambioRepository;
import com.colegio.shuji.usuario.infrastructure.repository.JpaNotificacionSolicitudCambioRepository;
import com.colegio.shuji.usuario.infrastructure.repository.JpaSolicitudCambioRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SolicitudCambioRepositoryAdapter implements SolicitudCambioRepositoryPort {

  private final JpaSolicitudCambioRepository solicitudRepository;
  private final JpaEventoSolicitudCambioRepository eventoRepository;
  private final JpaNotificacionSolicitudCambioRepository notificacionRepository;

  @Override
  public SolicitudCambio save(SolicitudCambio s) {
    SolicitudCambioEntity entity = toEntity(s);
    return toDomain(solicitudRepository.save(entity));
  }

  @Override
  public Optional<SolicitudCambio> findById(Long id) {
    return solicitudRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<SolicitudCambio> bloquearPorId(Long id) {
    return solicitudRepository.bloquearPorId(id).map(this::toDomain);
  }

  @Override
  public void bloquearSolicitante(Long solicitanteId) {
    solicitudRepository.bloquearSolicitante(solicitanteId);
  }

  @Override
  public Optional<SolicitudCambio> findBySolicitanteIdAndIdempotencyKey(
      Long solicitanteId, String idempotencyKey) {
    return solicitudRepository.findBySolicitanteIdAndIdempotencyKey(solicitanteId, idempotencyKey)
        .map(this::toDomain);
  }

  @Override
  public Optional<SolicitudCambio> findRecienteDuplicada(
      Long solicitanteId, String titulo, String modulo, OffsetDateTime desde) {
    return solicitudRepository
        .findRecientesDuplicadas(solicitanteId, titulo, modulo, desde)
        .stream()
        .findFirst()
        .map(this::toDomain);
  }

  @Override
  public List<SolicitudCambio> findBySolicitanteIdOrderByFechaSolicitudDesc(Long solicitanteId) {
    return solicitudRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(solicitanteId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<SolicitudCambio> findAllOrderByFechaSolicitudDesc() {
    return solicitudRepository.findAllByOrderByFechaSolicitudDesc().stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public long siguienteFolio() {
    return solicitudRepository.siguienteFolio();
  }

  @Override
  public EventoSolicitudCambio saveEvento(EventoSolicitudCambio e) {
    EventoSolicitudCambioEntity entity =
        EventoSolicitudCambioEntity.builder()
            .id(e.getId())
            .solicitudId(e.getSolicitudId())
            .actorId(e.getActorId())
            .actorNombre(e.getActorNombre())
            .actorRol(e.getActorRol())
            .estadoAnterior(e.getEstadoAnterior())
            .estadoNuevo(e.getEstadoNuevo())
            .tipoEvento(e.getTipoEvento())
            .comentario(e.getComentario())
            .visibleDireccion(e.getVisibleDireccion())
            .evidenciaUrl(e.getEvidenciaUrl())
            .fechaEvento(e.getFechaEvento())
            .build();
    return toEventoDomain(eventoRepository.save(entity));
  }

  @Override
  public Optional<EventoSolicitudCambio> findEventoById(Long eventoId) {
    return eventoRepository.findById(eventoId).map(this::toEventoDomain);
  }

  @Override
  public List<EventoSolicitudCambio> findEventosBySolicitudId(Long solicitudId) {
    return eventoRepository.findBySolicitudIdOrderByFechaEventoAscIdAsc(solicitudId).stream()
        .map(this::toEventoDomain)
        .toList();
  }

  @Override
  public NotificacionSolicitudCambio saveNotificacion(NotificacionSolicitudCambio n) {
    NotificacionSolicitudCambioEntity entity =
        NotificacionSolicitudCambioEntity.builder()
            .id(n.getId())
            .solicitudId(n.getSolicitudId())
            .eventoId(n.getEventoId())
            .destinatarioEmail(n.getDestinatarioEmail())
            .asunto(n.getAsunto())
            .cuerpoResumen(n.getCuerpoResumen())
            .estadoEnvio(n.getEstadoEnvio())
            .intentos(n.getIntentos())
            .ultimoError(n.getUltimoError())
            .claveIdempotencia(n.getClaveIdempotencia())
            .fechaIntento(n.getFechaIntento())
            .build();
    return toNotificacionDomain(notificacionRepository.save(entity));
  }

  @Override
  public Optional<NotificacionSolicitudCambio> bloquearNotificacionPorId(Long notificacionId) {
    return notificacionRepository.bloquearPorId(notificacionId).map(this::toNotificacionDomain);
  }

  @Override
  public List<NotificacionSolicitudCambio> findNotificacionesBySolicitudId(Long solicitudId) {
    return notificacionRepository
        .findBySolicitudIdOrderByFechaIntentoDescIdDesc(solicitudId)
        .stream()
        .map(this::toNotificacionDomain)
        .toList();
  }

  private SolicitudCambioEntity toEntity(SolicitudCambio s) {
    return SolicitudCambioEntity.builder()
        .id(s.getId())
        .codigo(s.getCodigo())
        .titulo(s.getTitulo())
        .modulo(s.getModulo())
        .motivoProblema(s.getMotivoProblema())
        .resultadoEsperado(s.getResultadoEsperado())
        .alcanceImpacto(s.getAlcanceImpacto())
        .evidenciaInicial(s.getEvidenciaInicial())
        .solicitanteId(s.getSolicitanteId())
        .solicitanteNombre(s.getSolicitanteNombre())
        .solicitanteEmail(s.getSolicitanteEmail())
        .estado(s.getEstado())
        .prioridad(s.getPrioridad())
        .impactoEvaluado(s.getImpactoEvaluado())
        .responsableId(s.getResponsableId())
        .responsableNombre(s.getResponsableNombre())
        .aprobadorNombre(s.getAprobadorNombre())
        .registradoDecisionPor(s.getRegistradoDecisionPor())
        .fechaDecision(s.getFechaDecision())
        .motivoDecision(s.getMotivoDecision())
        .resumenCambio(s.getResumenCambio())
        .enlaceEvidenciaTecnica(s.getEnlaceEvidenciaTecnica())
        .planReversion(s.getPlanReversion())
        .pruebaPosterior(s.getPruebaPosterior())
        .resultadoPrueba(s.getResultadoPrueba())
        .verificacionCierre(s.getVerificacionCierre())
        .cerradoPorNombre(s.getCerradoPorNombre())
        .idempotencyKey(s.getIdempotencyKey())
        .fechaSolicitud(s.getFechaSolicitud())
        .fechaActualizacion(s.getFechaActualizacion())
        .fechaCierre(s.getFechaCierre())
        .build();
  }

  private SolicitudCambio toDomain(SolicitudCambioEntity e) {
    return SolicitudCambio.builder()
        .id(e.getId())
        .codigo(e.getCodigo())
        .titulo(e.getTitulo())
        .modulo(e.getModulo())
        .motivoProblema(e.getMotivoProblema())
        .resultadoEsperado(e.getResultadoEsperado())
        .alcanceImpacto(e.getAlcanceImpacto())
        .evidenciaInicial(e.getEvidenciaInicial())
        .solicitanteId(e.getSolicitanteId())
        .solicitanteNombre(e.getSolicitanteNombre())
        .solicitanteEmail(e.getSolicitanteEmail())
        .estado(e.getEstado())
        .prioridad(e.getPrioridad())
        .impactoEvaluado(e.getImpactoEvaluado())
        .responsableId(e.getResponsableId())
        .responsableNombre(e.getResponsableNombre())
        .aprobadorNombre(e.getAprobadorNombre())
        .registradoDecisionPor(e.getRegistradoDecisionPor())
        .fechaDecision(e.getFechaDecision())
        .motivoDecision(e.getMotivoDecision())
        .resumenCambio(e.getResumenCambio())
        .enlaceEvidenciaTecnica(e.getEnlaceEvidenciaTecnica())
        .planReversion(e.getPlanReversion())
        .pruebaPosterior(e.getPruebaPosterior())
        .resultadoPrueba(e.getResultadoPrueba())
        .verificacionCierre(e.getVerificacionCierre())
        .cerradoPorNombre(e.getCerradoPorNombre())
        .idempotencyKey(e.getIdempotencyKey())
        .fechaSolicitud(e.getFechaSolicitud())
        .fechaActualizacion(e.getFechaActualizacion())
        .fechaCierre(e.getFechaCierre())
        .build();
  }

  private EventoSolicitudCambio toEventoDomain(EventoSolicitudCambioEntity e) {
    return EventoSolicitudCambio.builder()
        .id(e.getId())
        .solicitudId(e.getSolicitudId())
        .actorId(e.getActorId())
        .actorNombre(e.getActorNombre())
        .actorRol(e.getActorRol())
        .estadoAnterior(e.getEstadoAnterior())
        .estadoNuevo(e.getEstadoNuevo())
        .tipoEvento(e.getTipoEvento())
        .comentario(e.getComentario())
        .visibleDireccion(e.getVisibleDireccion())
        .evidenciaUrl(e.getEvidenciaUrl())
        .fechaEvento(e.getFechaEvento())
        .build();
  }

  private NotificacionSolicitudCambio toNotificacionDomain(NotificacionSolicitudCambioEntity n) {
    return NotificacionSolicitudCambio.builder()
        .id(n.getId())
        .solicitudId(n.getSolicitudId())
        .eventoId(n.getEventoId())
        .destinatarioEmail(n.getDestinatarioEmail())
        .asunto(n.getAsunto())
        .cuerpoResumen(n.getCuerpoResumen())
        .estadoEnvio(n.getEstadoEnvio())
        .intentos(n.getIntentos())
        .ultimoError(n.getUltimoError())
        .claveIdempotencia(n.getClaveIdempotencia())
        .fechaIntento(n.getFechaIntento())
        .build();
  }
}
