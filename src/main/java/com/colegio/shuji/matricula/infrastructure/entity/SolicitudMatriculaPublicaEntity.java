package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_matricula_publica")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudMatriculaPublicaEntity {
  @Id private UUID id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoSolicitudMatriculaPublica estado;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "numero_documento_estudiante", nullable = false, length = 8)
  private String numeroDocumentoEstudiante;

  @Column(name = "nombres_estudiante", nullable = false, length = 100)
  private String nombresEstudiante;

  @Column(name = "apellido_paterno_estudiante", nullable = false, length = 80)
  private String apellidoPaternoEstudiante;

  @Column(name = "apellido_materno_estudiante", nullable = false, length = 80)
  private String apellidoMaternoEstudiante;

  @Column(name = "fecha_nacimiento_estudiante", nullable = false)
  private java.time.LocalDate fechaNacimientoEstudiante;

  @Enumerated(EnumType.STRING)
  @Column(name = "genero_estudiante", nullable = false, length = 1)
  private Genero generoEstudiante;

  @Column(name = "numero_documento_apoderado", nullable = false, length = 8)
  private String numeroDocumentoApoderado;

  @Column(name = "nombres_apoderado", nullable = false, length = 100)
  private String nombresApoderado;

  @Column(name = "apellido_paterno_apoderado", nullable = false, length = 80)
  private String apellidoPaternoApoderado;

  @Column(name = "apellido_materno_apoderado", nullable = false, length = 80)
  private String apellidoMaternoApoderado;

  @Column(name = "celular_apoderado", nullable = false, length = 9)
  private String celularApoderado;

  @Column(name = "email_apoderado", length = 100)
  private String emailApoderado;

  @Column(name = "direccion_apoderado", nullable = false, length = 200)
  private String direccionApoderado;

  @Column(name = "ubigeo_apoderado", nullable = false, length = 6)
  private String ubigeoApoderado;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Parentesco parentesco;

  @Column(name = "consentimiento_gemini", nullable = false)
  private boolean consentimientoGemini;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_partida", nullable = false, length = 15)
  private EstadoValidacionDocumento estadoPartida = EstadoValidacionDocumento.PENDIENTE;

  @Column(name = "observacion_partida", length = 500)
  private String observacionPartida;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_dni_c4", nullable = false, length = 15)
  private EstadoValidacionDocumento estadoDniC4 = EstadoValidacionDocumento.PENDIENTE;

  @Column(name = "observacion_dni_c4", length = 500)
  private String observacionDniC4;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_recibo", nullable = false, length = 15)
  private EstadoValidacionDocumento estadoRecibo = EstadoValidacionDocumento.PENDIENTE;

  @Column(name = "observacion_recibo", length = 500)
  private String observacionRecibo;

  @Column(name = "pago_preferencia_id", length = 100)
  private String pagoPreferenciaId;

  @Column(name = "pago_enlace", length = 1000)
  private String pagoEnlace;

  @Column(name = "pago_id", length = 100)
  private String pagoId;

  @Column(name = "pago_monto", precision = 10, scale = 2, nullable = false)
  private BigDecimal pagoMonto = new BigDecimal("1.00");

  @Column(name = "pago_expira_at")
  private OffsetDateTime pagoExpiraAt;

  @Column(name = "vacante_reservada", nullable = false)
  private boolean vacanteReservada;

  @Column(name = "estudiante_id")
  private Long estudianteId;

  @Column(name = "apoderado_id")
  private Long apoderadoId;

  @Column(name = "matricula_id")
  private Long matriculaId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version private long version;

  @PrePersist
  void crearFechas() {
    var now = OffsetDateTime.now(ZoneOffset.UTC);
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  void actualizarFecha() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public EstadoValidacionDocumento estadoDocumento(
      com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud tipo) {
    return switch (tipo) {
      case PARTIDA_NACIMIENTO -> estadoPartida;
      case DNI_C4 -> estadoDniC4;
      case RECIBO_SERVICIO -> estadoRecibo;
    };
  }

  public String observacionDocumento(
      com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud tipo) {
    return switch (tipo) {
      case PARTIDA_NACIMIENTO -> observacionPartida;
      case DNI_C4 -> observacionDniC4;
      case RECIBO_SERVICIO -> observacionRecibo;
    };
  }

  public void actualizarDocumento(
      com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud tipo,
      EstadoValidacionDocumento estado,
      String observacion) {
    switch (tipo) {
      case PARTIDA_NACIMIENTO -> { estadoPartida = estado; observacionPartida = observacion; }
      case DNI_C4 -> { estadoDniC4 = estado; observacionDniC4 = observacion; }
      case RECIBO_SERVICIO -> { estadoRecibo = estado; observacionRecibo = observacion; }
    }
  }
}
