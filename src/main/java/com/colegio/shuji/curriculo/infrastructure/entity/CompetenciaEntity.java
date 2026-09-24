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
@Table(name = "competencias")
@Getter
@Setter
@NoArgsConstructor
public class CompetenciaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "area_id", nullable = false)
  private Short areaId;

  @Column(name = "numero_orden", nullable = false)
  private Short numeroOrden;

  @Column(name = "nombre", nullable = false, length = 50)
  private String nombre;

  @Column(name = "descripcion", columnDefinition = "text")
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "area_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.curriculo.infrastructure.entity.AreaCurricularEntity relacion0;

  @PrePersist
  void inicializar() {}
}
