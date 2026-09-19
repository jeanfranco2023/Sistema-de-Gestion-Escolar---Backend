package com.colegio.shuji.tesoreria.domain.model;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
public class ComprobantePago {
  private Long id;
  private UUID uuid;
  private Long pagoTransaccionId;
  private TipoComprobante tipoComprobante;
  private String serie;
  private Integer correlativo;
  private OffsetDateTime fechaEmision;
  private BigDecimal montoTotal;
  private EstadoComprobante estadoComprobante;
  private OffsetDateTime fechaAnulacion;
  private String motivoAnulacion;
  private String urlPdfComprobante;

  public void anular(String motivo, OffsetDateTime fecha) {
    this.estadoComprobante = EstadoComprobante.ANULADO;
    this.motivoAnulacion = motivo;
    this.fechaAnulacion = fecha;
  }

  public String numeroCompleto() {
    return serie + "-" + String.format("%08d", correlativo);
  }

  public boolean estaAnulado() {
    return this.estadoComprobante == EstadoComprobante.ANULADO;
  }

  public static ComprobantePago emitir(
      PagoTransaccion pago, TipoComprobante tipo, String serie, Integer correlativo) {
    if (pago == null || !pago.estaAprobado() || pago.getId() == null)
      throw new BusinessException("El pago debe estar aprobado");
    if (tipo == null
        || serie == null
        || !serie.matches("[BE][0-9]{3}")
        || correlativo == null
        || correlativo <= 0) throw new BusinessException("Numeración de comprobante inválida");
    return builder()
        .pagoTransaccionId(pago.getId())
        .tipoComprobante(tipo)
        .serie(serie)
        .correlativo(correlativo)
        .montoTotal(pago.getMontoPagado())
        .estadoComprobante(EstadoComprobante.EMITIDO)
        .build();
  }
}
