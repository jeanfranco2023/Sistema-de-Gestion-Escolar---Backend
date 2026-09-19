package com.colegio.shuji.asistencia.infrastructure.entity;

import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  @Column(name = "dispositivo_codigo", nullable = false, length = 30)
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
