package com.colegio.shuji.matricula.application.service;

import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagoMatriculaPublicaService {
  private final MercadoPagoPort mercadoPago;
  private final SolicitudMatriculaPublicaService solicitudes;

  public SolicitudMatriculaPublicaResponseDto confirmarRetorno(
      UUID solicitudId, String token, String paymentId) {
    solicitudes.validarToken(solicitudId, token);
    return solicitudes.finalizarPagoVerificado(
        solicitudId, token, mercadoPago.consultarPago(paymentId));
  }

  public boolean esPagoDeSolicitud(PagoVerificado pago) {
    return pago.referenciaExterna() != null
        && pago.referenciaExterna().startsWith(SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX);
  }

  public void procesarWebhook(PagoVerificado pago) {
    if (esPagoDeSolicitud(pago)) solicitudes.procesarPagoWebhook(pago);
  }
}
