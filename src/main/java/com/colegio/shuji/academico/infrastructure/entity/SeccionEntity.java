package com.colegio.shuji.academico.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "secciones")
@Getter
@Setter
@NoArgsConstructor
public class SeccionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "grado_id", nullable = false)
  private Short gradoId;

  @Column(name = "nivel_id", nullable = false)
  private Short nivelId;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "letra", nullable = false, length = 1)
  private String letra;

  @Column(name = "cupo_maximo", nullable = false)
  private Short cupoMaximo;

  @Column(name = "vacantes_ocupadas", nullable = false, insertable = false, updatable = false)
  private Short vacantesOcupadas;

  @Column(name = "aula_id", nullable = false)
  private Integer aulaId;

  @Column(name = "aula_fisica", length = 30)
  private String aulaFisica;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "aula_id", insertable = false, updatable = false)
  private AulaEntity aula;

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
        name = "grado_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "nivel_id",
        referencedColumnName = "nivel_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.GradoEntity relacion1;

  @PrePersist
  void inicializar() {
    if (vacantesOcupadas == null) vacantesOcupadas = (short) 0;
  }
}
