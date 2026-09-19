package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;
import com.colegio.shuji.tesoreria.domain.enums.*;

public interface ProcesarPagoPasarelaUseCase {
  TransaccionResponseDto registrarCaja(RegistrarPagoCajaRequestDto r);

  TransaccionResponseDto procesarWebhook(
      PasarelaProveedor proveedor, ProcesarPagoWebhookRequestDto r, String credencial);

  PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(CrearPreferenciaMercadoPagoDto r);
}
