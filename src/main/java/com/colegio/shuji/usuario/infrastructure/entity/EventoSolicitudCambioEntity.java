package com.colegio.shuji.usuario.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_cambio_eventos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoSolicitudCambioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "solicitud_id", nullable = false)
  private Long solicitudId;

  @Column(name = "actor_id")
  private Long actorId;

  @Column(name = "actor_nombre", nullable = false, length = 180)
  private String actorNombre;

  @Column(name = "actor_rol", nullable = false, length = 60)
  private String actorRol;

  @Column(name = "estado_anterior", length = 40)
  private String estadoAnterior;

  @Column(name = "estado_nuevo", length = 40)
  private String estadoNuevo;

  @Column(name = "tipo_evento", nullable = false, length = 60)
  private String tipoEvento;

  @Column(name = "comentario", length = 2000)
  private String comentario;

  @Column(name = "visible_direccion", nullable = false)
  private Boolean visibleDireccion;

  @Column(name = "evidencia_url", length = 500)
  private String evidenciaUrl;

  @Column(name = "fecha_evento", nullable = false)
  private OffsetDateTime fechaEvento;
}
