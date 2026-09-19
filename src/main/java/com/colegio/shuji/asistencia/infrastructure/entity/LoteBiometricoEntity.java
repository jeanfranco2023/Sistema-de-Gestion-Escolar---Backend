package com.colegio.shuji.asistencia.infrastructure.entity;

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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  @Column(name = "hash_contenido", nullable = false, unique = true, length = 64)
  private String hashContenido;

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
