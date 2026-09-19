package com.colegio.shuji.asistencia.infrastructure.entity;

import com.colegio.shuji.asistencia.domain.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "conciliaciones_asistencia")
@Getter
@Setter
@NoArgsConstructor
public class ConciliacionAsistenciaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "marco_porteria", nullable = false)
  private Boolean marcoPorteria;

  @Column(name = "presente_aula", nullable = false)
  private Boolean presenteAula;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_discrepancia", nullable = false, length = 35)
  private TipoDiscrepancia tipoDiscrepancia;

  @Column(name = "alerta_notificada", nullable = false)
  private Boolean alertaNotificada;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity relacion0;

  @PrePersist
  void inicializar() {
    if (alertaNotificada == null) alertaNotificada = false;
  }
}
