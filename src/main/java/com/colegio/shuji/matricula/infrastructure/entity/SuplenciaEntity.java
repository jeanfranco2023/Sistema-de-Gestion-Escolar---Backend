package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.EstadoSuplencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity @Table(name="apoderados_suplentes") @Getter @Setter @NoArgsConstructor
public class SuplenciaEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="estudiante_id",nullable=false) private Long estudianteId;
  @Column(name="apoderado_titular_id",nullable=false) private Long apoderadoTitularId;
  @Column(name="numero_documento_suplente",nullable=false,length=8) private String numeroDocumento;
  @Column(name="nombres_suplente",nullable=false,length=20) private String nombres;
  @Column(name="apellido_paterno_suplente",nullable=false,length=20) private String apellidoPaterno;
  @Column(name="apellido_materno_suplente",nullable=false,length=20) private String apellidoMaterno;
  @Column(name="parentesco_suplente",nullable=false,length=30) private String parentesco;
  @Column(name="celular_suplente",nullable=false,length=9) private String celular;
  @Column(name="fecha_inicio",nullable=false) private LocalDate fechaInicio;
  @Column(name="fecha_fin",nullable=false) private LocalDate fechaFin;
  @Enumerated(EnumType.STRING) @Column(name="estado",nullable=false,length=20) private EstadoSuplencia estado;
  @Column(name="motivo_resolucion",length=500) private String motivoResolucion;
  @Column(name="autorizado_por_usuario_id") private Long autorizadoPorUsuarioId;
  @Column(name="fecha_solicitud",nullable=false) private OffsetDateTime fechaSolicitud;
  @Column(name="fecha_resolucion") private OffsetDateTime fechaResolucion;
}
