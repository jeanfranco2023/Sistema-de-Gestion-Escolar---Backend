package com.colegio.shuji.comunicado.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "comunicados_oficiales")
@Getter
@Setter
@NoArgsConstructor
public class ComunicadoOficialEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "titulo", nullable = false, length = 150)
  private String titulo;

  @Column(name = "contenido", nullable = false, columnDefinition = "text")
  private String contenido;

  @Column(name = "remitente_usuario_id", nullable = false)
  private Long remitenteUsuarioId;

  @Column(name = "fecha_publicacion", nullable = false)
  private OffsetDateTime fechaPublicacion;

  @Column(name = "requiere_acuse", nullable = false)
  private Boolean requiereAcuse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "remitente_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion0;

  @PrePersist
  void inicializar() {
    if (fechaPublicacion == null) fechaPublicacion = OffsetDateTime.now(ZoneOffset.UTC);
    if (requiereAcuse == null) requiereAcuse = false;
  }
}
