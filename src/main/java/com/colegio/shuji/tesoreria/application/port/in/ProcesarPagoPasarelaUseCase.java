package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.in.ProcesarPagoWebhookRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;

public interface ProcesarPagoPasarelaUseCase {
  TransaccionResponseDto registrarCaja(RegistrarPagoCajaRequestDto r);

  TransaccionResponseDto procesarWebhook(
      PasarelaProveedor proveedor, ProcesarPagoWebhookRequestDto r, String credencial);

  PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(CrearPreferenciaMercadoPagoDto r);
}
