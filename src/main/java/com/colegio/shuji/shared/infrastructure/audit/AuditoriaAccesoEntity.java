package com.colegio.shuji.shared.infrastructure.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_accesos_api")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaAccesoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "solicitud_id", nullable = false, length = 36, unique = true)
  private String solicitudId;

  @Column(name = "usuario_id")
  private Long usuarioId;

  @Column(name = "usuario_nombre", nullable = false, length = 100)
  private String usuarioNombre;

  @Column(nullable = false, length = 300)
  private String roles;

  @Column(nullable = false, length = 10)
  private String metodo;

  @Column(nullable = false, length = 500)
  private String ruta;

  @Column(name = "estado_http", nullable = false)
  private Integer estadoHttp;

  @Column(name = "direccion_ip", length = 45)
  private String direccionIp;

  @Column(name = "agente_usuario", length = 300)
  private String agenteUsuario;

  @Column(name = "duracion_ms", nullable = false)
  private Long duracionMs;

  @Column(name = "fecha_evento", nullable = false, insertable = false, updatable = false)
  private Instant fechaEvento;

  @PrePersist
  void validarDatosMinimos() {
    if (usuarioNombre == null || usuarioNombre.isBlank()) usuarioNombre = "ANONIMO";
    if (roles == null || roles.isBlank()) roles = "ANONIMO";
  }
}
