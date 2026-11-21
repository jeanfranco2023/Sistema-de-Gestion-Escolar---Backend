package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.CrearSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.GestionarSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.in.ResponderInfoSolicitudCambioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.NotificacionSolicitudCambioResponseDto;
import com.colegio.shuji.usuario.application.dto.out.SolicitudCambioResponseDto;
import java.time.OffsetDateTime;
import java.util.List;

public interface GestionSolicitudCambioUseCase {

  SolicitudCambioResponseDto crearSolicitud(CrearSolicitudCambioRequestDto request);

  List<SolicitudCambioResponseDto> listarMisSolicitudes();

  List<SolicitudCambioResponseDto> listarTodas(
      String estado,
      String modulo,
      String responsable,
      OffsetDateTime fechaDesde,
      OffsetDateTime fechaHasta);

  SolicitudCambioResponseDto obtenerDetalle(Long id);

  SolicitudCambioResponseDto responderInformacion(
      Long id, ResponderInfoSolicitudCambioRequestDto request);

  SolicitudCambioResponseDto gestionarSolicitud(
      Long id, GestionarSolicitudCambioRequestDto request);

  NotificacionSolicitudCambioResponseDto reintentarNotificacion(Long id, Long notificacionId);
}
