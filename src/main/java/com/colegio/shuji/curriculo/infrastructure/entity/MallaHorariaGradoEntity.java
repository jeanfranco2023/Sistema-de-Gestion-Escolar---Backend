package com.colegio.shuji.curriculo.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mallas_horarias_grado")
@Getter
@Setter
@NoArgsConstructor
public class MallaHorariaGradoEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "grado_id", nullable = false)
  private Short gradoId;

  @Column(name = "area_curricular_id", nullable = false)
  private Short areaCurricularId;

  @Column(name = "dia_semana", nullable = false)
  private Short diaSemana;

  @Column(name = "bloque_horario_id", nullable = false)
  private Short bloqueHorarioId;

  @Column(name = "activo", nullable = false)
  private Boolean activo;
}
