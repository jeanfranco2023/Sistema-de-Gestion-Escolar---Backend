package com.colegio.shuji.convivencia.infrastructure.entity;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "historial_estados_incidencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstadoIncidenciaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "incidencia_id", nullable = false)
  private Long incidenciaId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_anterior", length = 20)
  private EstadoIncidencia estadoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo", nullable = false, length = 20)
  private EstadoIncidencia estadoNuevo;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "nombre_usuario_snapshot", nullable = false, length = 30)
  private String nombreUsuarioSnapshot;

  @Column(name = "fecha_cambio", nullable = false)
  private OffsetDateTime fechaCambio;

  @Column(name = "observacion_gestion", nullable = false, length = 1000)
  private String observacion;

  @Column(name = "resultado_verificado", length = 300)
  private String resultadoVerificado;

  @Column(name = "participantes", length = 120)
  private String participantes;

  @Column(name = "comunicacion_apoderado", length = 130)
  private String comunicacionApoderado;
}
