package com.colegio.shuji.evaluacion.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "sesiones_refuerzo")
@Getter
@Setter
@NoArgsConstructor
public class SesionRefuerzoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "periodo_academico_id", nullable = false)
  private Short periodoAcademicoId;

  @Column(name = "area_curricular_id", nullable = false)
  private Short areaCurricularId;

  @Column(name = "docente_usuario_id", nullable = false)
  private Long docenteUsuarioId;

  @Column(name = "tema", nullable = false, length = 150)
  private String tema;

  @Column(name = "fecha_programada", nullable = false)
  private LocalDate fechaProgramada;

  @Column(name = "hora_inicio", nullable = false)
  private LocalTime horaInicio;

  @Column(name = "hora_fin", nullable = false)
  private LocalTime horaFin;

  @Column(name = "aula_asignada", length = 30)
  private String aulaAsignada;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "area_curricular_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.AreaCurricularEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "docente_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "periodo_academico_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "anio_lectivo_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.PeriodoAcademicoEntity relacion2;

  @PrePersist
  void inicializar() {}
}
