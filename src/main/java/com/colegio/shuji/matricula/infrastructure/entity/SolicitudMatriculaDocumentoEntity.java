package com.colegio.shuji.matricula.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitudes_matricula_documentos")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudMatriculaDocumentoEntity {
  @EmbeddedId
  private SolicitudMatriculaDocumentoId id;

  @Column(name = "version_id", nullable = false)
  private java.util.UUID versionId;

  @Column(name = "contenedor", nullable = false, length = 100)
  private String bucket;

  @Column(name = "ruta_objeto", nullable = false, length = 500)
  private String objectKey;

  @Column(name = "tipo_contenido", nullable = false, length = 30)
  private String mimeType;

  @Column(name = "tamano_bytes", nullable = false)
  private long tamanoBytes;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "huella_sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;
}
