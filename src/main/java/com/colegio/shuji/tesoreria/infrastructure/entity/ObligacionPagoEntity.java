package com.colegio.shuji.tesoreria.infrastructure.entity;

import com.colegio.shuji.tesoreria.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "obligaciones_pago")
@Getter
@Setter
@NoArgsConstructor
public class ObligacionPagoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "matricula_id", nullable = false)
  private Long matriculaId;

  @Column(name = "concepto_id", nullable = false)
  private Short conceptoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_concepto", nullable = false, length = 20)
  private TipoConcepto tipoConcepto;

  @Column(name = "numero_cuota", nullable = false)
  private Short numeroCuota;

  @Column(name = "descripcion", nullable = false, length = 150)
  private String descripcion;

  @Column(name = "fecha_vencimiento", nullable = false)
  private LocalDate fechaVencimiento;

  @Column(name = "monto_base", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoBase;

  @Column(name = "monto_mora", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoMora;

  @Column(name = "monto_descuento", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoDescuento;

  @Column(
      name = "total_pagado",
      nullable = false,
      insertable = false,
      updatable = false,
      precision = 10,
      scale = 2)
  private BigDecimal totalPagado;

  @Column(
      name = "saldo_pendiente",
      insertable = false,
      updatable = false,
      precision = 10,
      scale = 2)
  private BigDecimal saldoPendiente;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 25)
  private EstadoObligacion estado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "matricula_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "concepto_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "tipo_concepto",
        referencedColumnName = "tipo_concepto",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.tesoreria.infrastructure.entity.ConceptoCobroEntity relacion1;

  @PrePersist
  void inicializar() {
    if (montoMora == null) montoMora = BigDecimal.ZERO;
    if (montoDescuento == null) montoDescuento = BigDecimal.ZERO;
    if (totalPagado == null) totalPagado = BigDecimal.ZERO;
    if (estado == null) estado = EstadoObligacion.PENDIENTE;
  }
}
