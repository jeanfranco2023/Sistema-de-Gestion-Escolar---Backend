package com.colegio.shuji.curriculo.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asignaciones_docentes")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionDocenteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "docente_usuario_id", nullable = false)
  private Long docenteUsuarioId;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "nivel_id", nullable = false)
  private Short nivelId;

  @Column(name = "area_curricular_id", nullable = false)
  private Short areaCurricularId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "docente_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "seccion_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "anio_lectivo_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "nivel_id",
        referencedColumnName = "nivel_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.SeccionEntity relacion1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "area_curricular_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "nivel_id",
        referencedColumnName = "nivel_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.AreaCurricularEntity relacion2;

  @PrePersist
  void inicializar() {}
}
