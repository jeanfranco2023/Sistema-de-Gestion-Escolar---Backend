package com.colegio.shuji.asistencia.infrastructure.entity;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
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
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asistencias_aula")
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaAulaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "matricula_id", nullable = false)
  private Long matriculaId;

  @Column(name = "fecha_sesion", nullable = false)
  private LocalDate fechaSesion;

  @Column(name = "hora_registro", nullable = false)
  private LocalTime horaRegistro;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 25)
  private EstadoAsistenciaAula estado;

  @Column(name = "auxiliar_usuario_id")
  private Long auxiliarUsuarioId;

  @Column(name = "justificada", nullable = false)
  private Boolean justificada;

  @Column(name = "motivo_justificacion", columnDefinition = "text")
  private String motivoJustificacion;

  @Column(name = "documento_sustento_url", length = 255)
  private String documentoSustentoUrl;

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
        name = "auxiliar_usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion1;

  @PrePersist
  void inicializar() {
    if (justificada == null) justificada = false;
  }
}
