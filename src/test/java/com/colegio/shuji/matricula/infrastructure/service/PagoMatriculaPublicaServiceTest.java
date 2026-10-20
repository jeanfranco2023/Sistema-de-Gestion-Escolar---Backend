package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PagoMatriculaPublicaServiceTest {
  final SolicitudMatriculaPublicaService solicitudes = mock(SolicitudMatriculaPublicaService.class);
  final MercadoPagoPort mercado = mock(MercadoPagoPort.class);
  final PagoMatriculaPublicaService servicio = new PagoMatriculaPublicaService(mercado, solicitudes);
  final UUID id = UUID.randomUUID();

  @Test void conciliaAunqueNoLlegueWebhookSinConfiarEnParametrosDelNavegador() {
    var pendiente = estado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    var finalizada = estado(EstadoSolicitudMatriculaPublica.MATRICULADA);
    var referencia = SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + id;
    var pago = new PagoVerificado("123", null, new BigDecimal("350.00"), "PEN", MetodoPago.YAPE, true, referencia);
    when(solicitudes.consultar(id, "token")).thenReturn(pendiente);
    when(mercado.buscarPagoAprobado(referencia)).thenReturn(Optional.of(pago));
    when(solicitudes.finalizarPagoVerificado(id, "token", pago)).thenReturn(finalizada);
    assertSame(finalizada, servicio.consultarPago(id, "token"));
  }

  @Test void noApruebaSiLaPasarelaAunNoConfirma() {
    var pendiente = estado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    when(solicitudes.consultar(id, "token")).thenReturn(pendiente);
    when(mercado.buscarPagoAprobado(anyString())).thenReturn(Optional.empty());
    assertSame(pendiente, servicio.consultarPago(id, "token"));
    verify(solicitudes, never()).finalizarPagoVerificado(any(), any(), any());
  }

  @Test void verificaTokenAntesDeConsultarProveedor() {
    when(solicitudes.consultar(id, "invalido")).thenThrow(new IllegalArgumentException("token"));
    assertThrows(IllegalArgumentException.class, () -> servicio.consultarPago(id, "invalido"));
    verifyNoInteractions(mercado);
  }

  @Test void recuperaConfirmacionDiferidaDespuesDeExpirarLaReserva() {
    var validada = estado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    var referencia = SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + id;
    var pago = new PagoVerificado("123", null, new BigDecimal("350.00"), "PEN", MetodoPago.PAGO_EFECTIVO, true, referencia);
    when(solicitudes.consultar(id, "token")).thenReturn(validada);
    when(mercado.buscarPagoAprobado(referencia)).thenReturn(Optional.of(pago));
    servicio.consultarPago(id, "token");
    verify(solicitudes).finalizarPagoVerificado(id, "token", pago);
  }

  private SolicitudMatriculaPublicaResponseDto estado(EstadoSolicitudMatriculaPublica estado) {
    return new SolicitudMatriculaPublicaResponseDto(id, null, estado, "Maria", List.of(), null,
        new BigDecimal("350.00"), null, null);
  }
}
