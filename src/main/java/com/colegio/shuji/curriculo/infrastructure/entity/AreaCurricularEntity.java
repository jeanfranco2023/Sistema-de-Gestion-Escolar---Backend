package com.colegio.shuji.curriculo.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "areas_curriculares")
@Getter
@Setter
@NoArgsConstructor
public class AreaCurricularEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "nivel_id", nullable = false)
  private Short nivelId;

  @Column(name = "codigo", nullable = false, length = 20)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "nivel_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.NivelEntity relacion0;

  @PrePersist
  void inicializar() {}
}
