package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.shared.domain.exception.UnauthorizedException;
import com.colegio.shuji.usuario.application.dto.in.CrearSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.GestionarSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.ResponderInfoSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.EventoSolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.dto.out.NotificacionSolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.dto.out.SolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.port.in.GestionSolicitudCambioUseCase;
import com.colegio.shuji.usuario.application.port.out.SolicitudCambioRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.enums.EstadoSolicitudCambio;
import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.model.EventoSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.NotificacionSolicitudCambio;
import com.colegio.shuji.usuario.domain.model.SolicitudCambio;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SolicitudCambioService implements GestionSolicitudCambioUseCase {

  private final SolicitudCambioRepositoryPort repository;
  private final UserRepositoryPort userRepository;
  private final NotificacionSolicitudCambioService notificaciones;
  private final ActorActualPort actor;
  private final AuditContextPort auditoria;

  @Override
  public SolicitudCambioResponseDto crearSolicitud(CrearSolicitudCambioRequestDto request) {
    Long usuarioId = actor.usuarioId();
    auditoria.setCurrentUserId(usuarioId);
    repository.bloquearSolicitante(usuarioId);

    Usuario solicitante =
        userRepository
            .findById(usuarioId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Usuario solicitante no encontrado: " + usuarioId));

    String claveIdempotencia =
        request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
            ? request.getIdempotencyKey().trim()
            : null;

    if (claveIdempotencia != null) {
      Optional<SolicitudCambio> existentePorClave =
          repository.findBySolicitanteIdAndIdempotencyKey(usuarioId, claveIdempotencia);
      if (existentePorClave.isPresent()) {
        return mapearDetalle(existentePorClave.get(), false);
      }
    } else {
      Optional<SolicitudCambio> duplicadaReciente =
          repository.findRecienteDuplicada(
              usuarioId, request.getTitulo().trim(), request.getModulo().trim(),
              OffsetDateTime.now().minusMinutes(5));
      if (duplicadaReciente.isPresent()) {
        return mapearDetalle(duplicadaReciente.get(), false);
      }
    }

    OffsetDateTime ahora = OffsetDateTime.now();
    String codigo = String.format(Locale.ROOT, "CH-%03d", repository.siguienteFolio());
    String nombreSolicitante =
        solicitante.getNombreCompleto() != null && !solicitante.getNombreCompleto().isBlank()
            ? solicitante.getNombreCompleto().trim()
            : solicitante.getUsername();
    String emailSolicitante =
        solicitante.getEmail() != null && !solicitante.getEmail().isBlank()
            ? solicitante.getEmail().trim()
            : "";

    PrioridadSolicitudCambio prioridad =
        request.getPrioridad() != null ? request.getPrioridad() : PrioridadSolicitudCambio.MEDIA;

    SolicitudCambio nueva =
        SolicitudCambio.builder()
            .codigo(codigo)
            .titulo(request.getTitulo().trim())
            .modulo(request.getModulo().trim())
            .motivoProblema(request.getMotivoProblema().trim())
            .resultadoEsperado(request.getResultadoEsperado().trim())
            .alcanceImpacto(
                request.getAlcanceImpacto() != null && !request.getAlcanceImpacto().isBlank()
                    ? request.getAlcanceImpacto().trim()
                    : null)
            .evidenciaInicial(
                request.getEvidenciaInicial() != null && !request.getEvidenciaInicial().isBlank()
                    ? request.getEvidenciaInicial().trim()
                    : null)
            .solicitanteId(usuarioId)
            .solicitanteNombre(nombreSolicitante)
            .solicitanteEmail(emailSolicitante)
            .estado(EstadoSolicitudCambio.RECIBIDA)
            .prioridad(prioridad)
            .idempotencyKey(
                claveIdempotencia != null ? claveIdempotencia : UUID.randomUUID().toString())
            .fechaSolicitud(ahora)
            .fechaActualizacion(ahora)
            .build();

    SolicitudCambio guardada = repository.save(nueva);

    EventoSolicitudCambio eventoInicial =
        repository.saveEvento(
            EventoSolicitudCambio.builder()
                .solicitudId(guardada.getId())
                .actorId(usuarioId)
                .actorNombre(nombreSolicitante)
                .actorRol("DIRECCION")
                .estadoAnterior(null)
                .estadoNuevo(EstadoSolicitudCambio.RECIBIDA.name())
                .tipoEvento("SOLICITUD_CREADA")
                .comentario("Solicitud de cambio registrada por Dirección.")
                .visibleDireccion(Boolean.TRUE)
                .evidenciaUrl(guardada.getEvidenciaInicial())
                .fechaEvento(ahora)
                .build());

    String asunto = guardada.getCodigo() + " · Solicitud de cambio recibida";
    String resumen =
        "Hemos registrado tu solicitud de cambio \""
            + guardada.getTitulo()
            + "\" para el módulo "
            + guardada.getModulo()
            + ". Estado actual: RECIBIDA. Puedes consultar el seguimiento en /admin/solicitudes-cambio.";
    notificaciones.encolar(guardada, eventoInicial, asunto, resumen);

    return mapearDetalle(guardada, false);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SolicitudCambioResponseDto> listarMisSolicitudes() {
    Long usuarioId = actor.usuarioId();
    return repository.findBySolicitanteIdOrderByFechaSolicitudDesc(usuarioId).stream()
        .map(s -> baseDto(s, false).build())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SolicitudCambioResponseDto> listarTodas(
      String estado,
      String modulo,
      String responsable,
      OffsetDateTime fechaDesde,
      OffsetDateTime fechaHasta) {
    return repository.findAllOrderByFechaSolicitudDesc().stream()
        .filter(
            s ->
                estado == null
                    || estado.isBlank()
                    || (s.getEstado() != null && s.getEstado().name().equalsIgnoreCase(estado.trim())))
        .filter(
            s ->
                modulo == null
                    || modulo.isBlank()
                    || (s.getModulo() != null
                        && s.getModulo().toLowerCase().contains(modulo.trim().toLowerCase())))
        .filter(
            s ->
                responsable == null
                    || responsable.isBlank()
                    || (s.getResponsableNombre() != null
                        && s.getResponsableNombre()
                            .toLowerCase()
                            .contains(responsable.trim().toLowerCase())))
        .filter(
            s ->
                fechaDesde == null
                    || (s.getFechaSolicitud() != null && !s.getFechaSolicitud().isBefore(fechaDesde)))
        .filter(
            s ->
                fechaHasta == null
                    || (s.getFechaSolicitud() != null && !s.getFechaSolicitud().isAfter(fechaHasta)))
        .map(s -> baseDto(s, true).build())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SolicitudCambioResponseDto obtenerDetalle(Long id) {
    SolicitudCambio solicitud =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Solicitud de cambio no encontrada con ID: " + id));

    boolean esAdmin = actor.tieneRol("ADMINISTRADOR");
    if (!esAdmin) {
      Long usuarioActualId = actor.usuarioId();
      if (!solicitud.getSolicitanteId().equals(usuarioActualId)) {
        throw new UnauthorizedException(
            "No tiene permisos para acceder a solicitudes de cambio de otros usuarios.");
      }
    }

    return mapearDetalle(solicitud, esAdmin);
  }

  @Override
  public SolicitudCambioResponseDto responderInformacion(
      Long id, ResponderInfoSolicitudCambioRequestDto request) {
    Long usuarioId = actor.usuarioId();
    auditoria.setCurrentUserId(usuarioId);

    SolicitudCambio solicitud =
        repository
            .bloquearPorId(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Solicitud de cambio no encontrada con ID: " + id));

    if (!solicitud.getSolicitanteId().equals(usuarioId)) {
      throw new UnauthorizedException(
          "Solo el solicitante propietario puede responder información en este ticket.");
    }

    if (solicitud.getEstado() != EstadoSolicitudCambio.REQUIERE_INFORMACION) {
      throw new ReglaNegocioException(
          "Solo se puede responder cuando la solicitud requiere información.",
          "ESTADO_NO_ADMITE_RESPUESTA");
    }

    String estadoAnterior = solicitud.getEstado().name();
    OffsetDateTime ahora = OffsetDateTime.now();
    solicitud.responderInformacion(request.getEvidenciaUrl(), ahora);
    SolicitudCambio actualizada = repository.save(solicitud);

    String nombreActor = obtenerNombreActor(usuarioId);
    repository.saveEvento(
        EventoSolicitudCambio.builder()
            .solicitudId(actualizada.getId())
            .actorId(usuarioId)
            .actorNombre(nombreActor)
            .actorRol("DIRECCION")
            .estadoAnterior(estadoAnterior)
            .estadoNuevo(actualizada.getEstado().name())
            .tipoEvento("INFORMACION_ADICIONAL")
            .comentario(request.getComentario().trim())
            .visibleDireccion(Boolean.TRUE)
            .evidenciaUrl(
                request.getEvidenciaUrl() != null && !request.getEvidenciaUrl().isBlank()
                    ? request.getEvidenciaUrl().trim()
                    : null)
            .fechaEvento(ahora)
            .build());

    return mapearDetalle(actualizada, false);
  }

  @Override
  public SolicitudCambioResponseDto gestionarSolicitud(
      Long id, GestionarSolicitudCambioRequestDto request) {
    Long adminId = actor.usuarioId();
    auditoria.setCurrentUserId(adminId);

    SolicitudCambio solicitud =
        repository
            .bloquearPorId(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Solicitud de cambio no encontrada con ID: " + id));

    EstadoSolicitudCambio estadoAnterior = solicitud.getEstado();
    EstadoSolicitudCambio nuevoEstado =
        request.getNuevoEstado() != null ? request.getNuevoEstado() : estadoAnterior;
    if (!estadoAnterior.admiteGestionHacia(nuevoEstado)) {
      throw new ReglaNegocioException(
          "La transición de " + estadoAnterior + " a " + nuevoEstado + " no está permitida.",
          "TRANSICION_SOLICITUD_NO_PERMITIDA");
    }

    boolean cambioEstado = estadoAnterior != nuevoEstado;
    boolean visibleDireccion = !Boolean.FALSE.equals(request.getVisibleDireccion());
    if (cambioEstado && nuevoEstado == EstadoSolicitudCambio.RECHAZADA) {
      boolean tieneMotivo =
          (request.getMotivoDecision() != null && !request.getMotivoDecision().isBlank())
              || (request.getComentario() != null && !request.getComentario().isBlank());
      if (!tieneMotivo) {
        throw new ReglaNegocioException(
            "Para rechazar una solicitud de cambio es obligatorio indicar el motivo de rechazo.",
            "MOTIVO_RECHAZO_OBLIGATORIO");
      }
    }

    if (cambioEstado && nuevoEstado == EstadoSolicitudCambio.REQUIERE_INFORMACION) {
      if (!visibleDireccion || request.getComentario() == null || request.getComentario().isBlank()) {
        throw new ReglaNegocioException(
            "Indique la información requerida en un comentario visible para Dirección.",
            "COMENTARIO_INFO_OBLIGATORIO");
      }
    }

    if (cambioEstado && nuevoEstado == EstadoSolicitudCambio.RESUELTA) {
      String resumenEfectivo =
          request.getResumenCambio() != null && !request.getResumenCambio().isBlank()
              ? request.getResumenCambio()
              : solicitud.getResumenCambio();
      String pruebaEfectiva =
          request.getPruebaPosterior() != null && !request.getPruebaPosterior().isBlank()
              ? request.getPruebaPosterior()
              : (request.getResultadoPrueba() != null && !request.getResultadoPrueba().isBlank()
                  ? request.getResultadoPrueba()
                  : solicitud.getPruebaPosterior());
      if (resumenEfectivo == null
          || resumenEfectivo.isBlank()
          || pruebaEfectiva == null
          || pruebaEfectiva.isBlank()) {
        throw new ReglaNegocioException(
            "Para marcar la solicitud como RESUELTA debe registrar el resumen del cambio y la verificación/prueba posterior.",
            "EVIDENCIA_RESOLUCION_OBLIGATORIA");
      }
    }

    OffsetDateTime ahora = OffsetDateTime.now();
    String nombreAdmin = obtenerNombreActor(adminId);

    String aprobadorNombre = request.getAprobadorNombre();
    String registradoDecisionPor = null;
    OffsetDateTime fechaDecision = null;
    String motivoDecision = request.getMotivoDecision();

    if (cambioEstado
        && (nuevoEstado == EstadoSolicitudCambio.APROBADA
            || nuevoEstado == EstadoSolicitudCambio.RECHAZADA)) {
      if (aprobadorNombre == null || aprobadorNombre.isBlank()) {
        aprobadorNombre = nombreAdmin;
      }
      registradoDecisionPor = nombreAdmin;
      fechaDecision = ahora;
      if ((motivoDecision == null || motivoDecision.isBlank())
          && request.getComentario() != null
          && !request.getComentario().isBlank()) {
        motivoDecision = request.getComentario().trim();
      }
    }

    String cerradoPorNombre = null;
    OffsetDateTime fechaCierre = null;
    if (nuevoEstado == EstadoSolicitudCambio.CERRADA) {
      cerradoPorNombre = nombreAdmin;
      fechaCierre = ahora;
    }

    Long responsableId =
        request.getResponsableId() != null
            ? request.getResponsableId()
            : (solicitud.getResponsableId() == null ? adminId : null);
    String responsableNombre =
        request.getResponsableNombre() != null && !request.getResponsableNombre().isBlank()
            ? request.getResponsableNombre()
            : (solicitud.getResponsableNombre() == null ? nombreAdmin : null);

    solicitud.aplicarGestion(
        nuevoEstado,
        request.getPrioridad(),
        request.getImpactoEvaluado(),
        responsableId,
        responsableNombre,
        aprobadorNombre,
        registradoDecisionPor,
        fechaDecision,
        motivoDecision,
        request.getResumenCambio(),
        request.getEnlaceEvidenciaTecnica(),
        request.getPlanReversion(),
        request.getPruebaPosterior(),
        request.getResultadoPrueba(),
        request.getVerificacionCierre(),
        cerradoPorNombre,
        fechaCierre,
        ahora);

    SolicitudCambio actualizada = repository.save(solicitud);

    String comentarioEvento =
        request.getComentario() != null && !request.getComentario().isBlank()
            ? request.getComentario().trim()
            : construirComentarioPorDefecto(nuevoEstado, request);

    EventoSolicitudCambio evento =
        repository.saveEvento(
            EventoSolicitudCambio.builder()
                .solicitudId(actualizada.getId())
                .actorId(adminId)
                .actorNombre(nombreAdmin)
                .actorRol("ADMINISTRADOR")
                .estadoAnterior(estadoAnterior != null ? estadoAnterior.name() : null)
                .estadoNuevo(nuevoEstado.name())
                .tipoEvento(
                    cambioEstado ? "CAMBIO_ESTADO" : "ACTUALIZACION_TECNICA")
                .comentario(comentarioEvento)
                .visibleDireccion(visibleDireccion)
                .evidenciaUrl(
                    request.getEvidenciaUrl() != null && !request.getEvidenciaUrl().isBlank()
                        ? request.getEvidenciaUrl().trim()
                         : null)
                .fechaEvento(ahora)
                .build());

    if (cambioEstado && requiereNotificacionCorreo(nuevoEstado)) {
      String asunto = construirAsuntoNotificacion(actualizada.getCodigo(), nuevoEstado);
      String cuerpoResumen =
          construirResumenNotificacion(
              actualizada, nuevoEstado, visibleDireccion ? comentarioEvento : null);
      notificaciones.encolar(actualizada, evento, asunto, cuerpoResumen);
    }

    return mapearDetalle(actualizada, true);
  }

  @Override
  public NotificacionSolicitudCambioResponseDto reintentarNotificacion(
      Long id, Long notificacionId) {
    Long adminId = actor.usuarioId();
    auditoria.setCurrentUserId(adminId);
    return mapearNotificacion(notificaciones.entregar(id, notificacionId), true);
  }

  private boolean requiereNotificacionCorreo(EstadoSolicitudCambio estado) {
    return estado == EstadoSolicitudCambio.REQUIERE_INFORMACION
        || estado == EstadoSolicitudCambio.APROBADA
        || estado == EstadoSolicitudCambio.RECHAZADA
        || estado == EstadoSolicitudCambio.RESUELTA
        || estado == EstadoSolicitudCambio.CERRADA;
  }

  private String construirAsuntoNotificacion(String codigo, EstadoSolicitudCambio estado) {
    return switch (estado) {
      case REQUIERE_INFORMACION -> codigo + " · Se requiere información adicional";
      case APROBADA -> codigo + " · Solicitud de cambio aprobada";
      case RECHAZADA -> codigo + " · Solicitud de cambio rechazada";
      case RESUELTA -> codigo + " · Tu solicitud de cambio fue atendida";
      case CERRADA -> codigo + " · Solicitud de cambio cerrada";
      default -> codigo + " · Actualización de solicitud de cambio";
    };
  }

  private String construirResumenNotificacion(
      SolicitudCambio solicitud, EstadoSolicitudCambio estado, String comentario) {
    StringBuilder sb = new StringBuilder();
    sb.append("Folio: ").append(solicitud.getCodigo()).append("\n");
    sb.append("Título: ").append(solicitud.getTitulo()).append("\n");
    sb.append("Estado actual: ").append(estado.name()).append("\n");
    if (comentario != null && !comentario.isBlank()) {
      sb.append("Detalle: ").append(comentario).append("\n");
    }
    sb.append("Consulta el seguimiento en: /admin/solicitudes-cambio");
    return sb.toString();
  }

  private String construirComentarioPorDefecto(
      EstadoSolicitudCambio estado, GestionarSolicitudCambioRequestDto request) {
    if (estado == EstadoSolicitudCambio.RESUELTA
        && request.getResumenCambio() != null && !request.getResumenCambio().isBlank()) {
      return request.getResumenCambio().trim();
    }
    if (estado == EstadoSolicitudCambio.RECHAZADA
        && request.getMotivoDecision() != null && !request.getMotivoDecision().isBlank()) {
      return request.getMotivoDecision().trim();
    }
    return "Actualización de estado a " + estado.name();
  }

  private String obtenerNombreActor(Long usuarioId) {
    return userRepository
        .findById(usuarioId)
        .map(
            u ->
                u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank()
                    ? u.getNombreCompleto().trim()
                    : u.getUsername())
        .orElseGet(() -> actor.nombreUsuario());
  }

  private SolicitudCambioResponseDto.SolicitudCambioResponseDtoBuilder baseDto(
      SolicitudCambio s, boolean incluirInternosAdmin) {
    return SolicitudCambioResponseDto.builder()
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
        .impactoEvaluado(incluirInternosAdmin ? s.getImpactoEvaluado() : null)
        .responsableId(s.getResponsableId())
        .responsableNombre(s.getResponsableNombre())
        .aprobadorNombre(s.getAprobadorNombre())
        .registradoDecisionPor(s.getRegistradoDecisionPor())
        .fechaDecision(s.getFechaDecision())
        .motivoDecision(incluirInternosAdmin ? s.getMotivoDecision() : null)
        .resumenCambio(incluirInternosAdmin ? s.getResumenCambio() : null)
        .enlaceEvidenciaTecnica(incluirInternosAdmin ? s.getEnlaceEvidenciaTecnica() : null)
        .planReversion(incluirInternosAdmin ? s.getPlanReversion() : null)
        .pruebaPosterior(incluirInternosAdmin ? s.getPruebaPosterior() : null)
        .resultadoPrueba(incluirInternosAdmin ? s.getResultadoPrueba() : null)
        .verificacionCierre(incluirInternosAdmin ? s.getVerificacionCierre() : null)
        .cerradoPorNombre(s.getCerradoPorNombre())
        .fechaSolicitud(s.getFechaSolicitud())
        .fechaActualizacion(s.getFechaActualizacion())
        .fechaCierre(s.getFechaCierre())
        .eventos(List.of())
        .notificaciones(List.of());
  }

  private SolicitudCambioResponseDto mapearDetalle(SolicitudCambio s, boolean incluirInternosAdmin) {
    List<EventoSolicitudCambioResponseDto> eventos =
        repository.findEventosBySolicitudId(s.getId()).stream()
            .filter(e -> incluirInternosAdmin || Boolean.TRUE.equals(e.getVisibleDireccion()))
            .map(
                e ->
                    EventoSolicitudCambioResponseDto.builder()
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
                        .evidenciaUrl(
                            incluirInternosAdmin
                                    || "DIRECCION".equals(e.getActorRol())
                                ? e.getEvidenciaUrl()
                                : null)
                        .fechaEvento(e.getFechaEvento())
                        .build())
            .toList();

    List<NotificacionSolicitudCambioResponseDto> notificaciones =
        repository.findNotificacionesBySolicitudId(s.getId()).stream()
            .map(n -> mapearNotificacion(n, incluirInternosAdmin))
            .toList();

    var respuesta = baseDto(s, incluirInternosAdmin);
    if (!incluirInternosAdmin) {
      for (EventoSolicitudCambioResponseDto evento : eventos) {
        if ("CAMBIO_ESTADO".equals(evento.getTipoEvento())) {
          if (EstadoSolicitudCambio.RESUELTA.name().equals(evento.getEstadoNuevo())) {
            respuesta.resumenCambio(evento.getComentario());
          } else if (EstadoSolicitudCambio.RECHAZADA.name().equals(evento.getEstadoNuevo())) {
            respuesta.motivoDecision(evento.getComentario());
          }
        }
      }
    }
    return respuesta
        .eventos(eventos)
        .notificaciones(notificaciones)
        .build();
  }

  private NotificacionSolicitudCambioResponseDto mapearNotificacion(
      NotificacionSolicitudCambio n, boolean incluirInternosAdmin) {
    return NotificacionSolicitudCambioResponseDto.builder()
        .id(n.getId())
        .solicitudId(n.getSolicitudId())
        .eventoId(n.getEventoId())
        .destinatarioEmail(incluirInternosAdmin ? n.getDestinatarioEmail() : null)
        .asunto(n.getAsunto())
        .cuerpoResumen(incluirInternosAdmin ? n.getCuerpoResumen() : null)
        .estadoEnvio(n.getEstadoEnvio())
        .intentos(n.getIntentos())
        .ultimoError(incluirInternosAdmin ? n.getUltimoError() : null)
        .claveIdempotencia(incluirInternosAdmin ? n.getClaveIdempotencia() : null)
        .fechaIntento(n.getFechaIntento())
        .build();
  }
}
