package com.colegio.shuji.academico.infrastructure.entity;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  @Column(name = "nombre", nullable = false, length = 30)
  private String nombre;

  @PrePersist
  void inicializar() {}
}
