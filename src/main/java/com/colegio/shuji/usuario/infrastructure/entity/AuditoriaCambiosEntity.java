package com.colegio.shuji.usuario.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA mapeada a la tabla 'auditoria_cambios' en PostgreSQL (Supabase). Registra eventos
 * generados por los triggers del sistema fn_auditar_cambios().
 */
@Entity
@Table(name = "auditoria_cambios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuario")
public class AuditoriaCambiosEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tabla_afectada", nullable = false, length = 50)
  private String tablaAfectada;

  @Column(name = "registro_id", nullable = false)
  private Long registroId;

  @Column(nullable = false, length = 10)
  private String accion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "datos_anteriores", columnDefinition = "jsonb")
  private String datosAnteriores;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "datos_nuevos", columnDefinition = "jsonb")
  private String datosNuevos;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id")
  private UsuarioEntity usuario;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}
