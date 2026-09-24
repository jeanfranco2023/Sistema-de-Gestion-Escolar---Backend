package com.colegio.shuji.matricula.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_matricula_documentos")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudMatriculaDocumentoEntity {
  @EmbeddedId
  private SolicitudMatriculaDocumentoId id;

  @Column(nullable = false, length = 100)
  private String bucket;

  @Column(name = "object_key", nullable = false, length = 500)
  private String objectKey;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "tamano_bytes", nullable = false)
  private long tamanoBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "actualizado_at", nullable = false)
  private OffsetDateTime actualizadoAt;
}
