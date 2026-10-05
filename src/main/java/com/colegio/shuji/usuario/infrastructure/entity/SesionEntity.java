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
import org.hibernate.annotations.ColumnTransformer;

/** Entidad JPA mapeada a la tabla 'sesiones' en PostgreSQL (Supabase). */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuario")
public class SesionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false)
  private UsuarioEntity usuario;

  @Column(name = "huella_token", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "direccion_ip", columnDefinition = "inet")
  @ColumnTransformer(write = "?::inet")
  private String ipAddress;

  @Column(name = "agente_usuario", columnDefinition = "text")
  private String userAgent;

  @Column(name = "fecha_expiracion", nullable = false)
  private LocalDateTime fechaExpiracion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @PrePersist
  public void prePersist() {
    if (this.fechaCreacion == null) {
      this.fechaCreacion = LocalDateTime.now();
    }
  }
}
