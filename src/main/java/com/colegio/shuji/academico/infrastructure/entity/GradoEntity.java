package com.colegio.shuji.academico.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grados")
@Getter
@Setter
@NoArgsConstructor
public class GradoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Column(name = "nivel_id", nullable = false)
  private Short nivelId;

  @Column(name = "numero_grado", nullable = false)
  private Short numeroGrado;

  @Column(name = "nombre", nullable = false, length = 50)
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
