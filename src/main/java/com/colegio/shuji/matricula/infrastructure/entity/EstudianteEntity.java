package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
public class EstudianteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "uuid", nullable = false)
  private UUID uuid;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", nullable = false, length = 10)
  private TipoDocumento tipoDocumento;

  @Column(name = "numero_documento", nullable = false, length = 15)
  private String numeroDocumento;

  @Column(name = "nombres", nullable = false, length = 100)
  private String nombres;

  @Column(name = "apellido_paterno", nullable = false, length = 20)
  private String apellidoPaterno;

  @Column(name = "apellido_materno", nullable = false, length = 20)
  private String apellidoMaterno;

  @Column(name = "fecha_nacimiento", nullable = false)
  private LocalDate fechaNacimiento;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Enumerated(EnumType.STRING)
  @Column(name = "genero", nullable = false, length = 1)
  private Genero genero;

  @Column(name = "codigo_estudiante_siagie", length = 14)
  private String codigoEstudianteSiagie;

  @Column(name = "validado_reniec", nullable = false)
  private Boolean validadoReniec;

  @Enumerated(EnumType.STRING)
  @Column(name = "origen_registro", nullable = false, length = 20)
  private OrigenRegistro origenRegistro;

  @Column(name = "grupo_sanguineo", length = 5)
  private String grupoSanguineo;

  @Column(name = "alergias_condiciones", columnDefinition = "text")
  private String alergiasCondiciones;

  @Column(name = "activo", nullable = false)
  private Boolean activo;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void inicializar() {
    if (uuid == null) uuid = UUID.randomUUID();
    if (tipoDocumento == null) tipoDocumento = TipoDocumento.DNI;
    if (validadoReniec == null) validadoReniec = false;
    if (origenRegistro == null) origenRegistro = OrigenRegistro.RENIEC_API;
    if (activo == null) activo = true;
    if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
