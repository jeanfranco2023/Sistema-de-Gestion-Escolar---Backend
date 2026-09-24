package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import java.util.UUID;

public interface GestionarPagoMatriculaPublicaUseCase {
  SolicitudMatriculaPublicaResponseDto confirmarRetorno(
      UUID solicitudId, String token, String paymentId);

  boolean esPagoDeSolicitud(PagoVerificado pago);

  void procesarWebhook(PagoVerificado pago);
}
