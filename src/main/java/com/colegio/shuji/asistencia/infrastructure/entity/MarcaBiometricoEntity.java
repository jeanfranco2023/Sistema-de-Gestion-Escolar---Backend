package com.colegio.shuji.asistencia.infrastructure.entity;

import com.colegio.shuji.asistencia.domain.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "marcas_biometrico_porteria")
@Getter
@Setter
@NoArgsConstructor
public class MarcaBiometricoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "lote_id")
  private Long loteId;

  @Column(name = "dni_leido", nullable = false, length = 15)
  private String dniLeido;

  @Column(name = "fecha_hora", nullable = false)
  private OffsetDateTime fechaHora;

  @Column(name = "dispositivo_codigo", length = 30)
  private String dispositivoCodigo;

  @Column(name = "estudiante_id")
  private Long estudianteId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_procesamiento", nullable = false, length = 25)
  private EstadoMarca estadoProcesamiento;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "lote_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.asistencia.infrastructure.entity.LoteBiometricoEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity relacion1;

  @PrePersist
  void inicializar() {
    if (dispositivoCodigo == null) dispositivoCodigo = "PORTERIA_01";
    if (estadoProcesamiento == null) estadoProcesamiento = EstadoMarca.PENDIENTE;
    if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
