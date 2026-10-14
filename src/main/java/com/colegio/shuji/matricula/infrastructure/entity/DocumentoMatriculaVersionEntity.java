package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "versiones_documentos_matricula")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoMatriculaVersionEntity {
  @Id private UUID id;
  @Column(name = "solicitud_id", nullable = false) private UUID solicitudId;
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 30) private TipoDocumentoSolicitud tipo;
  @Column(name = "contenedor", nullable = false, length = 100) private String bucket;
  @Column(name = "ruta_objeto", nullable = false, length = 500) private String objectKey;
  @Column(name = "tipo_contenido", nullable = false, length = 30) private String mimeType;
  @Column(name = "tamano_bytes", nullable = false) private long tamanoBytes;
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "huella_sha256", nullable = false, length = 64) private String sha256;
  @Column(name = "ia_valida") private Boolean iaValida;
  @Column(name = "dni_api_coincide") private Boolean dniApiCoincide;
  @Column(name = "observacion_ia", length = 500) private String observacionIa;
  @Enumerated(EnumType.STRING)
  @Column(name = "estado_firma", nullable = false, length = 20) private EstadoFirmaDigital estadoFirma;
  @Column(name = "detalle_firma", nullable = false, length = 500) private String detalleFirma;
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "certificado_sha256", length = 64) private String certificadoSha256;
  @Column(name = "fecha_carga", nullable = false) private OffsetDateTime fechaCarga;
}
