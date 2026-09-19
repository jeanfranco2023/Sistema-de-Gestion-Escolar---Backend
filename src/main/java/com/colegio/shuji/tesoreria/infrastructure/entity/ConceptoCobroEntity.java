package com.colegio.shuji.tesoreria.infrastructure.entity;

import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
