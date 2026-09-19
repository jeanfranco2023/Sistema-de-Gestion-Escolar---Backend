package com.colegio.shuji.convivencia.infrastructure.entity;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "incidencias_conductuales")
@Getter
@Setter
@NoArgsConstructor
public class IncidenciaConductualEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "matricula_id", nullable = false)
  private Long matriculaId;

  @Column(name = "fecha_incidencia", nullable = false)
  private LocalDate fechaIncidencia;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_falta", nullable = false, length = 20)
  private TipoFalta tipoFalta;

  @Column(name = "descripcion", nullable = false, columnDefinition = "text")
  private String descripcion;

  @Column(name = "reportado_por_usuario_id", nullable = false)
  private Long reportadoPorUsuarioId;

  @Column(name = "requiere_citacion", nullable = false)
  private Boolean requiereCitacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoIncidencia estado;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "matricula_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "reportado_por_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion1;

  @PrePersist
  void inicializar() {
    if (requiereCitacion == null) requiereCitacion = false;
    if (estado == null) estado = EstadoIncidencia.ABIERTA;
    if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
