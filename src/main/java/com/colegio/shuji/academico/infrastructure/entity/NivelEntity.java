package com.colegio.shuji.academico.infrastructure.entity;

import com.colegio.shuji.academico.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "niveles")
@Getter
@Setter
@NoArgsConstructor
public class NivelEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Short id;

  @Enumerated(EnumType.STRING)
  @Column(name = "codigo", nullable = false, length = 20)
  private NivelCodigo codigo;

  @Column(name = "nombre", nullable = false, length = 50)
  private String nombre;

  @PrePersist
  void inicializar() {}
}
