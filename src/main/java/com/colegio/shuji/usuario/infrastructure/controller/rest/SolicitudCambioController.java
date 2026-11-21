package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.usuario.application.dto.in.CrearSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.GestionarSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.ResponderInfoSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.NotificacionSolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.dto.out.SolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.port.in.GestionSolicitudCambioUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitudes-cambio")
@Validated
@RequiredArgsConstructor
@Tag(name = "Solicitudes de Cambio", description = "Gestión formal de Solicitudes de Cambio (RFC serie CH-)")
public class SolicitudCambioController {

  private final GestionSolicitudCambioUseCase solicitudesCambioUseCase;

  @PostMapping
  @PreAuthorize("hasRole('DIRECCION')")
  @Operation(summary = "Registrar una nueva solicitud de cambio institucional (exclusivo Dirección)")
  public ResponseEntity<SolicitudCambioResponseDto> crear(
      @Valid @RequestBody CrearSolicitudCambioRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(solicitudesCambioUseCase.crearSolicitud(request));
  }

  @GetMapping("/mias")
  @PreAuthorize("hasRole('DIRECCION')")
  @Operation(summary = "Listar las solicitudes de cambio propias de Dirección")
  public ResponseEntity<List<SolicitudCambioResponseDto>> listarMias() {
    return ResponseEntity.ok(solicitudesCambioUseCase.listarMisSolicitudes());
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Listar todas las solicitudes de cambio con filtros (exclusivo Administrador)")
  public ResponseEntity<List<SolicitudCambioResponseDto>> listarTodas(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String modulo,
      @RequestParam(required = false) String responsable,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime fechaDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime fechaHasta) {
    return ResponseEntity.ok(
        solicitudesCambioUseCase.listarTodas(estado, modulo, responsable, fechaDesde, fechaHasta));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "Obtener el detalle y trazabilidad de una solicitud de cambio")
  public ResponseEntity<SolicitudCambioResponseDto> obtenerDetalle(
      @PathVariable @Positive Long id) {
    return ResponseEntity.ok(solicitudesCambioUseCase.obtenerDetalle(id));
  }

  @PostMapping("/{id}/informacion")
  @PreAuthorize("hasRole('DIRECCION')")
  @Operation(summary = "Responder solicitud de información adicional (Dirección propietaria)")
  public ResponseEntity<SolicitudCambioResponseDto> responderInformacion(
      @PathVariable @Positive Long id,
      @Valid @RequestBody ResponderInfoSolicitudCambioRequestDto request) {
    return ResponseEntity.ok(solicitudesCambioUseCase.responderInformacion(id, request));
  }

  @PatchMapping("/{id}/gestion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Evaluar, transicionar estado o documentar resolución de solicitud de cambio (Administrador)")
  public ResponseEntity<SolicitudCambioResponseDto> gestionar(
      @PathVariable @Positive Long id,
      @Valid @RequestBody GestionarSolicitudCambioRequestDto request) {
    return ResponseEntity.ok(solicitudesCambioUseCase.gestionarSolicitud(id, request));
  }

  @PostMapping("/{id}/notificaciones/{notificacionId}/reintentar")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Reintentar envío de notificación por correo fallida (Administrador)")
  public ResponseEntity<NotificacionSolicitudCambioResponseDto> reintentarNotificacion(
      @PathVariable @Positive Long id,
      @PathVariable @Positive Long notificacionId) {
    return ResponseEntity.ok(solicitudesCambioUseCase.reintentarNotificacion(id, notificacionId));
  }
}
