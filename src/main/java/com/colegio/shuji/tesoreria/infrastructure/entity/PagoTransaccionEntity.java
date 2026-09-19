package com.colegio.shuji.tesoreria.infrastructure.entity;

import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pagos_transacciones")
@Getter
@Setter
@NoArgsConstructor
public class PagoTransaccionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "uuid", nullable = false)
  private UUID uuid;

  @Column(name = "obligacion_pago_id", nullable = false)
  private Long obligacionPagoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "pasarela_proveedor", nullable = false, length = 30)
  private PasarelaProveedor pasarelaProveedor;

  @Column(name = "pasarela_transaccion_id", nullable = false, length = 100)
  private String pasarelaTransaccionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "metodo_pago", nullable = false, length = 30)
  private MetodoPago metodoPago;

  @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoPagado;

  @Column(name = "fecha_pago", nullable = false)
  private OffsetDateTime fechaPago;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_pago", nullable = false, length = 20)
  private EstadoPago estadoPago;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "payload_webhook", columnDefinition = "jsonb")
  private Map<String, Object> payloadWebhook;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "obligacion_pago_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.tesoreria.infrastructure.entity.ObligacionPagoEntity relacion0;

  @PrePersist
  void inicializar() {
    if (uuid == null) uuid = UUID.randomUUID();
    if (fechaPago == null) fechaPago = OffsetDateTime.now(ZoneOffset.UTC);
    if (estadoPago == null) estadoPago = EstadoPago.APROBADO;
  }
}
