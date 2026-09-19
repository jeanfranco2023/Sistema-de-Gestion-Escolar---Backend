package com.colegio.shuji.tesoreria.domain.model;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoTransaccion {
  private Long id;
  private UUID uuid;
  private Long obligacionPagoId;
  private PasarelaProveedor pasarelaProveedor;
  private String pasarelaTransaccionId;
  private MetodoPago metodoPago;
  private BigDecimal montoPagado;
  private OffsetDateTime fechaPago;
  private EstadoPago estadoPago;
  private Map<String, Object> payloadWebhook;

  public void revertir() {
    if (estadoPago != EstadoPago.APROBADO) {
      throw new BusinessException("Solo se revierten pagos aprobados");
    }
    this.estadoPago = EstadoPago.REVERTIDO;
  }

  public void aprobar() {
    if (estadoPago == EstadoPago.REVERTIDO)
      throw new BusinessException("Un pago revertido es definitivo");
    if (montoPagado == null || montoPagado.signum() <= 0)
      throw new BusinessException("Importe inválido");
    this.estadoPago = EstadoPago.APROBADO;
  }

  public void rechazar() {
    if (estadoPago == EstadoPago.APROBADO || estadoPago == EstadoPago.REVERTIDO)
      throw new BusinessException("Debe revertirse el pago aprobado");
    this.estadoPago = EstadoPago.RECHAZADO;
  }

  public boolean estaAprobado() {
    return this.estadoPago == EstadoPago.APROBADO;
  }

  public void registrarOrigenCaja() {
    if (metodoPago != MetodoPago.EFECTIVO && metodoPago != MetodoPago.TRANSFERENCIA)
      throw new BusinessException("Método de caja inválido");
    pasarelaProveedor =
        metodoPago == MetodoPago.EFECTIVO
            ? PasarelaProveedor.CAJA_EFECTIVO
            : PasarelaProveedor.TRANSFERENCIA;
  }

  public void revertir(String motivo) {
    if (motivo == null || motivo.isBlank())
      throw new BusinessException("Indique motivo de reversión");
    revertir();
    var metadata = new java.util.HashMap<String, Object>();
    if (payloadWebhook != null) metadata.putAll(payloadWebhook);
    metadata.put("motivoReversion", motivo);
    payloadWebhook = metadata;
  }
}
