package com.colegio.shuji.academico.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "anios_lectivos")
@Getter
@Setter
@NoArgsConstructor
public class AnioLectivoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "anio", nullable = false)
  private Short anio;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Column(name = "abierto", nullable = false)
  private Boolean abierto;

  @PrePersist
  void inicializar() {
    if (abierto == null) abierto = true;
  }
}
