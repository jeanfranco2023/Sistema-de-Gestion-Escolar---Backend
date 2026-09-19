package com.colegio.shuji.asistencia.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "lotes_biometrico")
@Getter
@Setter
@NoArgsConstructor
public class LoteBiometricoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "nombre_archivo", nullable = false, length = 150)
  private String nombreArchivo;

  @Column(name = "total_filas", nullable = false)
  private Integer totalFilas;

  @Column(name = "marcas_validas", nullable = false)
  private Integer marcasValidas;

  @Column(name = "marcas_erroneas", nullable = false)
  private Integer marcasErroneas;

  @Column(name = "importado_por_usuario_id")
  private Long importadoPorUsuarioId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "importado_por_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion0;

  @PrePersist
  void inicializar() {
    if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
