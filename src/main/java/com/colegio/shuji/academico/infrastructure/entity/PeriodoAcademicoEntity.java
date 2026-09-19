package com.colegio.shuji.academico.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "periodos_academicos")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoAcademicoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "numero_periodo", nullable = false)
  private Short numeroPeriodo;

  @Column(name = "nombre", nullable = false, length = 30)
  private String nombre;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Column(name = "cerrado", nullable = false)
  private Boolean cerrado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity relacion0;

  @PrePersist
  void inicializar() {
    if (cerrado == null) cerrado = false;
  }
}
