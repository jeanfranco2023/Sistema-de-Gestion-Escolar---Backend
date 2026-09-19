package com.colegio.shuji.tesoreria.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.tesoreria.application.dto.in.EmitirComprobanteRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ComprobanteRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ComprobanteService implements EmitirComprobanteUseCase {
  private final TesoreriaMapper mapper;
  private final ComprobanteRepositoryPort comprobantes;
  private final PagoRepositoryPort pagos;

  public ComprobanteResponseDto emitir(EmitirComprobanteRequestDto r) {
    var pago = requerido(pagos.bloquearPorId(r.pagoId()));
    exigir(pago.getEstadoPago() == EstadoPago.APROBADO, "El pago debe estar aprobado");
    if (!comprobantes.buscarPorPagoTransaccionId(r.pagoId()).isEmpty())
      throw new com.colegio.shuji.tesoreria.domain.exception.ComprobanteYaEmitidoException(
          "El pago ya tiene comprobante");
    var c = ComprobantePago.emitir(pago, r.tipoComprobante(), r.serie(), r.correlativo());
    return mapper.toResponse(comprobantes.guardar(c));
  }

  public ComprobanteResponseDto emitirAutomaticoParaPago(Long pagoId, TipoComprobante tipo, String serie) {
    var pago = requerido(pagos.bloquearPorId(pagoId));
    exigir(pago.getEstadoPago() == EstadoPago.APROBADO, "El pago debe estar aprobado");
    var existentes = comprobantes.buscarPorPagoTransaccionId(pagoId);
    if (!existentes.isEmpty()) {
      return mapper.toResponse(existentes.getFirst());
    }
    int baseCorrelativo = comprobantes.obtenerUltimoCorrelativo(serie).orElse(0);
    DataIntegrityViolationException colisionException = null;
    for (int intento = 0; intento < 3; intento++) {
      var concurrentes = comprobantes.buscarPorPagoTransaccionId(pagoId);
      if (!concurrentes.isEmpty()) {
        return mapper.toResponse(concurrentes.getFirst());
      }
      int correlativo = baseCorrelativo + 1 + intento;
      var c = ComprobantePago.emitir(pago, tipo, serie, correlativo);
      try {
        return mapper.toResponse(comprobantes.guardar(c));
      } catch (DataIntegrityViolationException ex) {
        colisionException = ex;
        var despues = comprobantes.buscarPorPagoTransaccionId(pagoId);
        if (!despues.isEmpty()) {
          return mapper.toResponse(despues.getFirst());
        }
        baseCorrelativo = comprobantes.obtenerUltimoCorrelativo(serie).orElse(baseCorrelativo + intento);
      }
    }
    throw colisionException != null
        ? colisionException
        : new com.colegio.shuji.shared.domain.exception.BusinessException(
            "No se pudo emitir el comprobante tras múltiples intentos");
  }

  @Transactional(readOnly = true)
  public ComprobanteResponseDto consultar(Long id) {
    return mapper.toResponse(requerido(comprobantes.buscarPorId(id)));
  }
}
