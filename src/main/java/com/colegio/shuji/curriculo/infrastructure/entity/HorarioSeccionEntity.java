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
@Table(name = "horarios_seccion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioSeccionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "docente_usuario_id", nullable = false)
  private Long docenteUsuarioId;

  @Column(name = "asignacion_docente_id", nullable = false)
  private Long asignacionDocenteId;

  @Column(name = "dia_semana", nullable = false)
  private Short diaSemana;

  @Column(name = "bloque_horario_id", nullable = false)
  private Short bloqueHorarioId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity relacion0;

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
        name = "bloque_horario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.BloqueHorarioEntity relacion2;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "asignacion_docente_id",
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
        updatable = false),
    @JoinColumn(
        name = "docente_usuario_id",
        referencedColumnName = "docente_usuario_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.AsignacionDocenteEntity relacion3;

  @PrePersist
  void inicializar() {}
}
