package com.colegio.shuji.usuario.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Entidad JPA mapeada a la tabla 'usuarios' en PostgreSQL (Supabase). */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuarioPassword", "roles"})
public class UsuarioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  @Column(name = "nombre_usuario", nullable = false, unique = true, length = 30)
  private String username;

  @Column(name = "correo_electronico", nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "usuario_password", nullable = false, length = 60)
  private String usuarioPassword;

  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "usuario_roles",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "rol_id"))
  @Builder.Default
  private Set<RolEntity> roles = new HashSet<>();

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private LocalDateTime fechaActualizacion;

  @PrePersist
  public void prePersist() {
    if (this.activo == null) {
      this.activo = true;
    }
    LocalDateTime now = LocalDateTime.now();
    if (this.fechaCreacion == null) {
      this.fechaCreacion = now;
    }
    this.fechaActualizacion = now;
  }

  @PreUpdate
  public void preUpdate() {
    this.fechaActualizacion = LocalDateTime.now();
  }
}
