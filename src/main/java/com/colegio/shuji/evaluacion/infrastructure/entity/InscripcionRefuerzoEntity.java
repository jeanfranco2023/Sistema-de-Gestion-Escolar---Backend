package com.colegio.shuji.evaluacion.infrastructure.entity;

import com.colegio.shuji.evaluacion.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inscripciones_refuerzo")
@Getter
@Setter
@NoArgsConstructor
public class InscripcionRefuerzoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "sesion_refuerzo_id", nullable = false)
  private Long sesionRefuerzoId;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "calificacion_origen_id")
  private Long calificacionOrigenId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_asistencia", nullable = false, length = 20)
  private EstadoAsistenciaRefuerzo estadoAsistencia;

  @Column(name = "observaciones", columnDefinition = "text")
  private String observaciones;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "sesion_refuerzo_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.evaluacion.infrastructure.entity.SesionRefuerzoEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity relacion1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "calificacion_origen_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.evaluacion.infrastructure.entity.CalificacionCnebEntity relacion2;

  @PrePersist
  void inicializar() {
    if (estadoAsistencia == null) estadoAsistencia = EstadoAsistenciaRefuerzo.PENDIENTE;
  }
}
