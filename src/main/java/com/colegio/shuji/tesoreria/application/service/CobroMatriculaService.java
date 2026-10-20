package com.colegio.shuji.tesoreria.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CobroMatriculaService implements RegistrarCobroMatriculaUseCase {
  private final ConceptoCobroRepositoryPort conceptos;
  private final ObligacionRepositoryPort obligaciones;
  private final PagoRepositoryPort pagos;
  private final MatriculaRepositoryPort matriculas;
  private final EmitirComprobanteUseCase comprobantes;

  public String registrar(Long matriculaId, BigDecimal montoEsperado, PagoVerificado confirmado) {
    exigir(confirmado != null && confirmado.aprobado() && "PEN".equals(confirmado.moneda())
        && confirmado.monto() != null && confirmado.monto().compareTo(montoEsperado) == 0,
        "El pago de matrícula no coincide con el importe esperado en soles");
    requerido(matriculas.bloquearPorId(matriculaId));
    var concepto = requerido(conceptos.listar().stream()
        .filter(c -> c.getTipoConcepto() == TipoConcepto.MATRICULA).findFirst());
    var obligacion = obligaciones.buscarPorMatriculaId(matriculaId).stream()
        .filter(o -> o.getConceptoId().equals(concepto.getId()) && o.getNumeroCuota() == 0)
        .findFirst().orElseGet(() -> {
          // El importe cobrado es el reservado en el checkout, aunque cambie el tarifario después.
          var nueva = ObligacionPago.programar(matriculaId, concepto, (short) 0,
              LocalDate.now(ZoneId.of("America/Lima")), montoEsperado);
          return obligaciones.guardar(nueva);
        });
    obligacion = requerido(obligaciones.bloquearPorId(obligacion.getId()));
    var anteriores = pagos.buscarPorPasarelaTransaccionId(confirmado.transaccionId());
    PagoTransaccion pago;
    if (!anteriores.isEmpty()) {
      pago = anteriores.getFirst();
      exigir(pago.getObligacionPagoId().equals(obligacion.getId())
          && pago.getMontoPagado().compareTo(montoEsperado) == 0
          && pago.getPasarelaProveedor() == PasarelaProveedor.MERCADO_PAGO && pago.estaAprobado(),
          "El identificador de pago ya pertenece a otra operación");
    } else {
      obligacion.validarPago(confirmado.monto());
      pago = PagoTransaccion.builder().obligacionPagoId(obligacion.getId())
          .pasarelaProveedor(PasarelaProveedor.MERCADO_PAGO).pasarelaTransaccionId(confirmado.transaccionId())
          .montoPagado(confirmado.monto()).metodoPago(confirmado.metodo()).build();
      pago.aprobar();
      pago = pagos.guardar(pago);
    }
    var comprobante = comprobantes.emitirAutomaticoParaPago(pago.getId(), TipoComprobante.BOLETA, "B001");
    return comprobante.serie() + "-" + String.format(java.util.Locale.ROOT, "%08d", comprobante.correlativo());
  }
}
