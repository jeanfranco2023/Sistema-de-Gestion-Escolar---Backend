package com.colegio.shuji.curriculo.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "bloques_horarios")
@Getter
@Setter
@NoArgsConstructor
public class BloqueHorarioEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "numero_bloque", nullable = false)
  private Short numeroBloque;

  @Column(name = "hora_inicio", nullable = false)
  private LocalTime horaInicio;

  @Column(name = "hora_fin", nullable = false)
  private LocalTime horaFin;

  @Column(name = "es_recreo", nullable = false)
  private Boolean esRecreo;

  @PrePersist
  void inicializar() {
    if (esRecreo == null) esRecreo = false;
  }
}
