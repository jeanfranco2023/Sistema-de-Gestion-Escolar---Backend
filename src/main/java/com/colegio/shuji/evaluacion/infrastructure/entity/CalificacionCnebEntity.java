package com.colegio.shuji.evaluacion.infrastructure.entity;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "calificaciones_cneb")
@Getter
@Setter
@NoArgsConstructor
public class CalificacionCnebEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "matricula_id", nullable = false)
  private Long matriculaId;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "periodo_academico_id", nullable = false)
  private Short periodoAcademicoId;

  @Column(name = "asignacion_docente_id", nullable = false)
  private Long asignacionDocenteId;

  @Column(name = "area_curricular_id", nullable = false)
  private Short areaCurricularId;

  @Column(name = "docente_usuario_id", nullable = false)
  private Long docenteUsuarioId;

  @Column(name = "competencia_id", nullable = false)
  private Short competenciaId;

  @Enumerated(EnumType.STRING)
  @Column(name = "calificacion_cualitativa", nullable = false, length = 2)
  private CalificacionCualitativa calificacionCualitativa;

  @Column(name = "conclusion_descriptiva", columnDefinition = "text")
  private String conclusionDescriptiva;

  @Column(name = "sugerencia_ia_utilizada", nullable = false)
  private Boolean sugerenciaIaUtilizada;

  @Column(name = "requiere_refuerzo", insertable = false, updatable = false)
  private Boolean requiereRefuerzo;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "matricula_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "anio_lectivo_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "seccion_id",
        referencedColumnName = "seccion_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity relacion0;

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
  private com.colegio.shuji.academico.infrastructure.entity.PeriodoAcademicoEntity relacion1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "asignacion_docente_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "seccion_id",
        referencedColumnName = "seccion_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "area_curricular_id",
        referencedColumnName = "area_curricular_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "anio_lectivo_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "docente_usuario_id",
        referencedColumnName = "docente_usuario_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.AsignacionDocenteEntity relacion2;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "competencia_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "area_curricular_id",
        referencedColumnName = "area_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.CompetenciaEntity relacion3;

  @PrePersist
  void inicializar() {
    if (sugerenciaIaUtilizada == null) sugerenciaIaUtilizada = false;
    if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    if (updatedAt == null) updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
