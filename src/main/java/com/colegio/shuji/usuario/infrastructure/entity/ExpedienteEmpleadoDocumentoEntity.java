package com.colegio.shuji.usuario.infrastructure.entity;

import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "expedientes_empleados_documentos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteEmpleadoDocumentoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", nullable = false, length = 30)
  private TipoDocumentoEmpleado tipoDocumento;

  @Column(name = "nombre_archivo", nullable = false, length = 200)
  private String nombreArchivo;

  @Column(name = "ruta_objeto", nullable = false, length = 255)
  private String rutaObjeto;

  @Column(name = "tipo_contenido", nullable = false, length = 50)
  @Builder.Default
  private String tipoContenido = "application/pdf";

  @Column(name = "tamano_bytes", nullable = false)
  private Long tamanoBytes;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_ia", nullable = false, length = 20)
  @Builder.Default
  private EstadoValidacionIaEmpleado estadoIa = EstadoValidacionIaEmpleado.PENDIENTE;

  @Column(name = "observaciones_ia", columnDefinition = "TEXT")
  private String observacionesIa;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadatos_ia", columnDefinition = "jsonb")
  private String metadatosIa;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_validacion_ia")
  private OffsetDateTime fechaValidacionIa;

  @PrePersist
  public void prePersist() {
    if (this.fechaCreacion == null) {
      this.fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC);
    }
    if (this.estadoIa == null) {
      this.estadoIa = EstadoValidacionIaEmpleado.PENDIENTE;
    }
    if (this.tipoContenido == null) {
      this.tipoContenido = "application/pdf";
    }
  }
}
