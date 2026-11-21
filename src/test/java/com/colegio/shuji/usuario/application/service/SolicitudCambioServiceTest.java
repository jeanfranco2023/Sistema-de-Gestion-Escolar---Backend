package com.colegio.shuji.usuario.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.UnauthorizedException;
import com.colegio.shuji.usuario.application.dto.in.CrearSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.GestionarSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.ResponderInfoSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.NotificacionSolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.dto.out.SolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.port.out.SolicitudCambioCorreoPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SolicitudCambioServiceTest {

  @Mock private SolicitudCambioRepositoryPort repository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private SolicitudCambioCorreoPort correoPort;
  @Mock private ActorActualPort actor;
  @Mock private AuditContextPort auditoria;
  @Mock private ApplicationEventPublisher eventos;

  private SolicitudCambioService service;
  private NotificacionSolicitudCambioService notificador;

  private Usuario usuarioDireccion;
  private Usuario usuarioAdmin;
  private List<EventoSolicitudCambio> eventosGuardados;
  private List<NotificacionSolicitudCambio> notificacionesGuardadas;

  @BeforeEach
  void setUp() {
    notificador = new NotificacionSolicitudCambioService(repository, correoPort, eventos);
    service = new SolicitudCambioService(repository, userRepository, notificador, actor, auditoria);
    usuarioDireccion =
        Usuario.builder()
            .id(1L)
            .username("direccion")
            .nombreCompleto("Directora General")
            .email("direccion@shujikitamura.edu.pe")
            .activo(true)
            .build();
    usuarioAdmin =
        Usuario.builder()
            .id(2L)
            .username("admin")
            .nombreCompleto("Administrador del Sistema")
            .email("admin@shujikitamura.edu.pe")
            .activo(true)
            .build();
    eventosGuardados = new ArrayList<>();
    notificacionesGuardadas = new ArrayList<>();
  }

  @Test
  @DisplayName("Dirección crea solicitud de cambio comenzando en CH-026 con estado RECIBIDA")
  void crearSolicitudGeneraFolioCh026YNotifica() {
    when(actor.usuarioId()).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioDireccion));
    when(repository.findRecienteDuplicada(any(), anyString(), anyString(), any()))
        .thenReturn(Optional.empty());
    when(repository.siguienteFolio()).thenReturn(26L);
    when(repository.save(any(SolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              SolicitudCambio s = inv.getArgument(0);
              s.setId(26L);
              return s;
            });
    when(repository.saveEvento(any(EventoSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              EventoSolicitudCambio e = inv.getArgument(0);
              e.setId(100L);
              eventosGuardados.add(e);
              return e;
            });
    when(repository.saveNotificacion(any(NotificacionSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              NotificacionSolicitudCambio n = inv.getArgument(0);
              n.setId(200L);
              notificacionesGuardadas.add(n);
              return n;
            });
    when(repository.findEventosBySolicitudId(26L)).thenAnswer(inv -> eventosGuardados);
    when(repository.findNotificacionesBySolicitudId(26L)).thenAnswer(inv -> notificacionesGuardadas);

    CrearSolicitudCambioRequestDto req =
        CrearSolicitudCambioRequestDto.builder()
            .titulo("Habilitar exportación consolidada de indicadores")
            .modulo("Dashboard / Evaluación")
            .motivoProblema("Se requiere comparar la cobertura por grado al cierre bimestral.")
            .resultadoEsperado("Contar con el indicador consolidado por grado y sección.")
            .prioridad(PrioridadSolicitudCambio.ALTA)
            .build();

    SolicitudCambioResponseDto resp = service.crearSolicitud(req);

    assertNotNull(resp);
    assertEquals("CH-026", resp.getCodigo());
    assertEquals(EstadoSolicitudCambio.RECIBIDA, resp.getEstado());
    assertEquals(1, resp.getEventos().size());
    assertEquals(1, resp.getNotificaciones().size());
    assertEquals("PENDIENTE", resp.getNotificaciones().get(0).getEstadoEnvio());
    assertEquals(0, resp.getNotificaciones().get(0).getIntentos());
    verify(repository).bloquearSolicitante(1L);
    verify(eventos).publishEvent(new NotificacionSolicitudCambioService.NotificacionCreada(26L, 200L));
    verifyNoInteractions(correoPort);
  }

  @Test
  void reintentoDeAltaConsultaLaClaveDelSolicitanteYNoConsumeFolio() {
    var existente = SolicitudCambio.builder().id(26L).codigo("CH-026").solicitanteId(1L).build();
    when(actor.usuarioId()).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioDireccion));
    when(repository.findBySolicitanteIdAndIdempotencyKey(1L, "clave-formulario"))
        .thenReturn(Optional.of(existente));

    var resultado =
        service.crearSolicitud(
            CrearSolicitudCambioRequestDto.builder().idempotencyKey("clave-formulario").build());

    assertEquals(26L, resultado.getId());
    verify(repository).bloquearSolicitante(1L);
    verify(repository, never()).siguienteFolio();
    verifyNoInteractions(eventos, correoPort);
  }

  @Test
  @DisplayName("Dirección no puede consultar el detalle de un ticket que pertenece a otro usuario")
  void direccionNoPuedeVerTicketAjeno() {
    SolicitudCambio ajena =
        SolicitudCambio.builder()
            .id(30L)
            .codigo("CH-030")
            .solicitanteId(99L)
            .estado(EstadoSolicitudCambio.RECIBIDA)
            .build();

    when(repository.findById(30L)).thenReturn(Optional.of(ajena));
    when(actor.tieneRol("ADMINISTRADOR")).thenReturn(false);
    when(actor.usuarioId()).thenReturn(1L);

    assertThrows(UnauthorizedException.class, () -> service.obtenerDetalle(30L));
  }

  @Test
  @DisplayName("Flujo completo de estados por Administrador y respuesta de información por Dirección")
  void flujoCompletoEstadosAdministradorYDireccion() {
    SolicitudCambio ticket =
        SolicitudCambio.builder()
            .id(26L)
            .codigo("CH-026")
            .titulo("Ajuste en reporte de asistencia")
            .modulo("Asistencia")
            .motivoProblema("Diferencia de redondeo en corte mensual")
            .resultadoEsperado("Mostrar porcentaje con un decimal")
            .solicitanteId(1L)
            .solicitanteNombre("Directora General")
            .solicitanteEmail("direccion@shujikitamura.edu.pe")
            .estado(EstadoSolicitudCambio.RECIBIDA)
            .prioridad(PrioridadSolicitudCambio.MEDIA)
            .fechaSolicitud(OffsetDateTime.now())
            .fechaActualizacion(OffsetDateTime.now())
            .build();

    when(repository.bloquearPorId(26L)).thenReturn(Optional.of(ticket));
    when(repository.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));
    when(repository.saveEvento(any(EventoSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              EventoSolicitudCambio e = inv.getArgument(0);
              e.setId((long) (eventosGuardados.size() + 1));
              eventosGuardados.add(e);
              return e;
            });
    when(repository.saveNotificacion(any(NotificacionSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              NotificacionSolicitudCambio n = inv.getArgument(0);
              n.setId((long) (notificacionesGuardadas.size() + 1));
              notificacionesGuardadas.add(n);
              return n;
            });
    when(repository.findEventosBySolicitudId(26L)).thenAnswer(inv -> eventosGuardados);
    when(repository.findNotificacionesBySolicitudId(26L)).thenAnswer(inv -> notificacionesGuardadas);

    // 1. Admin -> EN_EVALUACION
    when(actor.usuarioId()).thenReturn(2L);
    when(userRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
    SolicitudCambioResponseDto r1 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.EN_EVALUACION)
                .impactoEvaluado("Impacto bajo en vista de indicadores")
                .comentario("Iniciando evaluación técnica")
                .build());
    assertEquals(EstadoSolicitudCambio.EN_EVALUACION, r1.getEstado());

    // 2. Admin -> REQUIERE_INFORMACION
    SolicitudCambioResponseDto r2 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.REQUIERE_INFORMACION)
                .comentario("¿Se requiere también en el filtro por sección?")
                .build());
    assertEquals(EstadoSolicitudCambio.REQUIERE_INFORMACION, r2.getEstado());

    // 3. Dirección responde información -> vuelve a EN_EVALUACION
    when(actor.usuarioId()).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioDireccion));
    SolicitudCambioResponseDto r3 =
        service.responderInformacion(
            26L,
            ResponderInfoSolicitudCambioRequestDto.builder()
                .comentario("Sí, incluir el filtro por sección.")
                .build());
    assertEquals(EstadoSolicitudCambio.EN_EVALUACION, r3.getEstado());

    // 4. Admin -> APROBADA
    when(actor.usuarioId()).thenReturn(2L);
    SolicitudCambioResponseDto r4 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.APROBADA)
                .aprobadorNombre("Comité de Cambios / Dirección")
                .motivoDecision("Cambio aprobado para despliegue")
                .build());
    assertEquals(EstadoSolicitudCambio.APROBADA, r4.getEstado());
    assertEquals("Comité de Cambios / Dirección", r4.getAprobadorNombre());
    assertEquals("Administrador del Sistema", r4.getRegistradoDecisionPor());

    // 5. Admin -> EN_EJECUCION
    SolicitudCambioResponseDto r5 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.EN_EJECUCION)
                .planReversion("Revertir commit del endpoint de indicadores")
                .build());
    assertEquals(EstadoSolicitudCambio.EN_EJECUCION, r5.getEstado());

    // 6. Admin -> RESUELTA (exige resumenCambio y pruebaPosterior)
    assertThrows(
        ReglaNegocioException.class,
        () ->
            service.gestionarSolicitud(
                26L,
                GestionarSolicitudCambioRequestDto.builder()
                    .nuevoEstado(EstadoSolicitudCambio.RESUELTA)
                    .build()));

    SolicitudCambioResponseDto r6 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.RESUELTA)
                .resumenCambio("Se ajustó el cálculo de asistencia con precisión decimal.")
                .pruebaPosterior("DashboardControllerTest en verde")
                .resultadoPrueba("CONFORME")
                .enlaceEvidenciaTecnica("PR #128")
                .build());
    assertEquals(EstadoSolicitudCambio.RESUELTA, r6.getEstado());

    // 7. Admin -> CERRADA
    SolicitudCambioResponseDto r7 =
        service.gestionarSolicitud(
            26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.CERRADA)
                .verificacionCierre("Verificado en producción por Dirección")
                .build());
    assertEquals(EstadoSolicitudCambio.CERRADA, r7.getEstado());
    assertNotNull(r7.getFechaCierre());
    assertEquals("Administrador del Sistema", r7.getCerradoPorNombre());
  }

  @Test
  void unTicketRecibidoNoSePuedeCerrarNiResponderAntesDeLaEvaluacion() {
    var ticket =
        SolicitudCambio.builder().id(26L).solicitanteId(1L)
            .estado(EstadoSolicitudCambio.RECIBIDA).build();
    when(actor.usuarioId()).thenReturn(1L);
    when(repository.bloquearPorId(26L)).thenReturn(Optional.of(ticket));

    assertThrows(ReglaNegocioException.class,
        () -> service.gestionarSolicitud(26L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.CERRADA).build()));
    assertThrows(ReglaNegocioException.class,
        () -> service.responderInformacion(26L,
            ResponderInfoSolicitudCambioRequestDto.builder().comentario("Aclaración").build()));
    assertEquals(EstadoSolicitudCambio.RECIBIDA, ticket.getEstado());
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Rechazo exige motivo obligatorio y fallo SMTP registra FALLIDO permitiendo reintento")
  void rechazoExigeMotivoYReintentoNotificacionFunciona() {
    SolicitudCambio ticket =
        SolicitudCambio.builder()
            .id(27L)
            .codigo("CH-027")
            .titulo("Cambio sin sustento")
            .modulo("Tesorería")
            .solicitanteId(1L)
            .solicitanteNombre("Directora General")
            .solicitanteEmail("direccion@shujikitamura.edu.pe")
            .estado(EstadoSolicitudCambio.EN_EVALUACION)
            .prioridad(PrioridadSolicitudCambio.BAJA)
            .build();

    when(actor.usuarioId()).thenReturn(2L);
    when(repository.bloquearPorId(27L)).thenReturn(Optional.of(ticket));

    // Rechazo sin motivo falla
    assertThrows(
        ReglaNegocioException.class,
        () ->
            service.gestionarSolicitud(
                27L,
                GestionarSolicitudCambioRequestDto.builder()
                    .nuevoEstado(EstadoSolicitudCambio.RECHAZADA)
                    .build()));

    // Rechazo con motivo pero simulando fallo SMTP no revierte transición
    when(userRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
    when(repository.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));
    when(repository.saveEvento(any(EventoSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              EventoSolicitudCambio e = inv.getArgument(0);
              e.setId(50L);
              eventosGuardados.add(e);
              return e;
            });
    doThrow(new IllegalStateException("SMTP timeout"))
        .doNothing()
        .when(correoPort)
        .enviarNotificacionSolicitudCambio(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    when(repository.saveNotificacion(any(NotificacionSolicitudCambio.class)))
        .thenAnswer(
            inv -> {
              NotificacionSolicitudCambio n = inv.getArgument(0);
              n.setId(88L);
              if (notificacionesGuardadas.isEmpty()) {
                notificacionesGuardadas.add(n);
              }
              return n;
            });
    when(repository.findEventosBySolicitudId(27L)).thenAnswer(inv -> eventosGuardados);
    when(repository.findNotificacionesBySolicitudId(27L)).thenAnswer(inv -> notificacionesGuardadas);

    SolicitudCambioResponseDto rechazado =
        service.gestionarSolicitud(
            27L,
            GestionarSolicitudCambioRequestDto.builder()
                .nuevoEstado(EstadoSolicitudCambio.RECHAZADA)
                .motivoDecision("Fuera del alcance normativo del año lectivo actual")
                 .visibleDireccion(false)
                .build());

    assertEquals(EstadoSolicitudCambio.RECHAZADA, rechazado.getEstado());
    assertEquals("PENDIENTE", rechazado.getNotificaciones().get(0).getEstadoEnvio());

    when(repository.findById(27L)).thenReturn(Optional.of(ticket));
    when(repository.bloquearNotificacionPorId(88L))
        .thenReturn(Optional.of(notificacionesGuardadas.get(0)));
    when(repository.findEventoById(50L)).thenReturn(Optional.of(eventosGuardados.get(0)));
    notificador.entregarConfirmada(
        new NotificacionSolicitudCambioService.NotificacionCreada(27L, 88L));
    assertEquals("FALLIDO", notificacionesGuardadas.get(0).getEstadoEnvio());

    NotificacionSolicitudCambioResponseDto reintentada =
        service.reintentarNotificacion(27L, 88L);
    assertEquals("ENVIADO", reintentada.getEstadoEnvio());
    assertEquals(2, reintentada.getIntentos());
    assertEquals("ENVIADO", service.reintentarNotificacion(27L, 88L).getEstadoEnvio());
    verify(correoPort, times(2)).enviarNotificacionSolicitudCambio(
        anyString(), anyString(), anyString(), eq("RECHAZADA"), anyString(), eq(""));

    when(actor.usuarioId()).thenReturn(1L);
    var paraDireccion = service.obtenerDetalle(27L);
    assertNull(paraDireccion.getMotivoDecision());
    assertEquals(0, paraDireccion.getEventos().size());
    assertNull(paraDireccion.getNotificaciones().get(0).getCuerpoResumen());
    assertNull(paraDireccion.getNotificaciones().get(0).getClaveIdempotencia());
  }
}
