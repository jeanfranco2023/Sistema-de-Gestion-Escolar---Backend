package com.colegio.shuji.tesoreria.infrastructure.entity;

import com.colegio.shuji.tesoreria.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "comprobantes_pago")
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "uuid", nullable = false)
  private UUID uuid;

  @Column(name = "pago_transaccion_id", nullable = false)
  private Long pagoTransaccionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_comprobante", nullable = false, length = 20)
  private TipoComprobante tipoComprobante;

  @Column(name = "serie", nullable = false, length = 4)
  private String serie;

  @Column(name = "correlativo", nullable = false)
  private Integer correlativo;

  @Column(name = "fecha_emision", nullable = false)
  private OffsetDateTime fechaEmision;

  @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoTotal;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_comprobante", nullable = false, length = 20)
  private EstadoComprobante estadoComprobante;

  @Column(name = "fecha_anulacion")
  private OffsetDateTime fechaAnulacion;

  @Column(name = "motivo_anulacion", length = 255)
  private String motivoAnulacion;

  @Column(name = "url_pdf_comprobante", length = 255)
  private String urlPdfComprobante;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "pago_transaccion_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.tesoreria.infrastructure.entity.PagoTransaccionEntity relacion0;

  @PrePersist
  void inicializar() {
    if (uuid == null) uuid = UUID.randomUUID();
    if (tipoComprobante == null) tipoComprobante = TipoComprobante.RECIBO_INTERNO;
    if (fechaEmision == null) fechaEmision = OffsetDateTime.now(ZoneOffset.UTC);
    if (estadoComprobante == null) estadoComprobante = EstadoComprobante.EMITIDO;
  }
}
