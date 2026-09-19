package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "matriculas")
@Getter
@Setter
@NoArgsConstructor
public class MatriculaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "uuid", nullable = false)
  private UUID uuid;

  @Column(name = "anio_lectivo_id", nullable = false)
  private Short anioLectivoId;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "fecha_matricula", nullable = false)
  private OffsetDateTime fechaMatricula;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_matricula", nullable = false, length = 25)
  private EstadoMatricula estadoMatricula;

  @Column(name = "reserva_expira_at")
  private OffsetDateTime reservaExpiraAt;

  @Column(name = "observaciones", columnDefinition = "text")
  private String observaciones;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "seccion_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "anio_lectivo_id",
        referencedColumnName = "anio_lectivo_id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.academico.infrastructure.entity.SeccionEntity relacion1;

  @PrePersist
  void inicializar() {
    if (uuid == null) uuid = UUID.randomUUID();
    if (fechaMatricula == null) fechaMatricula = OffsetDateTime.now(ZoneOffset.UTC);
    if (estadoMatricula == null) estadoMatricula = EstadoMatricula.SOLICITADA;
  }
}
