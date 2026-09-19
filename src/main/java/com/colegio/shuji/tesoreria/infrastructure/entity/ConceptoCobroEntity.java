package com.colegio.shuji.tesoreria.infrastructure.entity;

import com.colegio.shuji.tesoreria.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "conceptos_cobro")
@Getter
@Setter
@NoArgsConstructor
public class ConceptoCobroEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "codigo", nullable = false, length = 30)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_concepto", nullable = false, length = 20)
  private TipoConcepto tipoConcepto;

  @Column(name = "monto_sugerido", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoSugerido;

  @PrePersist
  void inicializar() {}
}
