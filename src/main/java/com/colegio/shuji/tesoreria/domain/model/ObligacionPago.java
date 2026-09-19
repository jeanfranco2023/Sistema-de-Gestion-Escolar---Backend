package com.colegio.shuji.tesoreria.domain.model;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ObligacionPago {
  private Long id;
  private Long matriculaId;
  private Short conceptoId;
  private TipoConcepto tipoConcepto;
  private Short numeroCuota;
  private String descripcion;
  private LocalDate fechaVencimiento;
  private BigDecimal montoBase;
  private BigDecimal montoMora;
  private BigDecimal montoDescuento;
  private BigDecimal totalPagado;
  private BigDecimal saldoPendiente;
  private EstadoObligacion estado;

  public BigDecimal saldo() {
    return montoBase.add(montoMora).subtract(montoDescuento).subtract(totalPagado);
  }

  public void validarPago(BigDecimal monto) {
    if (estado == EstadoObligacion.CANCELADO_POR_TRASLADO || monto.signum() <= 0) {
      throw new BusinessException("Obligación o importe inválido");
    }
    if (monto.compareTo(saldo()) > 0) {
      throw new com.colegio.shuji.tesoreria.domain.exception.SobrepagoException(
          "El importe excede el saldo");
    }
  }

  public void aplicarDescuento(BigDecimal descuento) {
    if (descuento != null && descuento.signum() >= 0) {
      this.montoDescuento = descuento;
    }
  }

  public void aplicarMora(BigDecimal mora) {
    if (mora != null && mora.signum() >= 0) {
      this.montoMora = mora;
    }
  }

  public void registrarAbono(BigDecimal monto) {
    validarPago(monto);
    this.totalPagado = this.totalPagado.add(monto);
    if (saldo().compareTo(BigDecimal.ZERO) == 0) {
      this.estado = EstadoObligacion.PAGADO_TOTAL;
    } else {
      this.estado = EstadoObligacion.PAGADO_PARCIAL;
    }
  }

  public void anularPorTraslado() {
    this.estado = EstadoObligacion.CANCELADO_POR_TRASLADO;
  }

  public boolean estaPagadoTotal() {
    return this.estado == EstadoObligacion.PAGADO_TOTAL;
  }

  public boolean estaPendiente() {
    return this.estado == EstadoObligacion.PENDIENTE;
  }

  public static ObligacionPago programar(
      Long matriculaId, ConceptoCobro concepto, short cuota, java.time.LocalDate vencimiento) {
    if (matriculaId == null || concepto == null || vencimiento == null || cuota < 0 || cuota > 10)
      throw new BusinessException("Datos de obligación inválidos");
    if ((concepto.getTipoConcepto() == TipoConcepto.MATRICULA && cuota != 0)
        || (concepto.getTipoConcepto() == TipoConcepto.PENSION && cuota == 0))
      throw new BusinessException("Cuota incompatible con el concepto");
    if (concepto.getMontoSugerido() == null || concepto.getMontoSugerido().signum() < 0)
      throw new BusinessException("Importe inválido");
    return builder()
        .matriculaId(matriculaId)
        .conceptoId(concepto.getId())
        .tipoConcepto(concepto.getTipoConcepto())
        .numeroCuota(cuota)
        .descripcion(concepto.getNombre() + " - " + cuota)
        .fechaVencimiento(vencimiento)
        .montoBase(concepto.getMontoSugerido())
        .montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO)
        .totalPagado(BigDecimal.ZERO)
        .estado(EstadoObligacion.PENDIENTE)
        .build();
  }
}
