package com.colegio.shuji.usuario.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Modelo puro del Dominio que representa la cuenta de un Usuario en el sistema. Contiene reglas de
 * negocio esenciales sin acoplamiento a infraestructura.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuarioPassword")
@EqualsAndHashCode(of = "id")
public class Usuario {

  private Long id;
  private String username;
  private String nombreCompleto;
  private String email;
  private String usuarioPassword;
  private Boolean activo;
  @Builder.Default private Boolean debeCambiarPassword = false;
  private String dni;
  private String telefono;
  private String direccion;
  private String especialidad;
  private String codigoModular;
  private String numeroColegiatura;
  private java.time.LocalDate fechaIngreso;
  private String tipoContrato;
  @Builder.Default private String estadoLaboral = "ACTIVO";

  @Builder.Default private Set<Rol> roles = new HashSet<>();

  private LocalDateTime fechaCreacion;
  private LocalDateTime fechaActualizacion;

  public boolean tieneRol(String codigoRol) {
    if (roles == null || codigoRol == null) {
      return false;
    }
    return roles.stream().anyMatch(r -> codigoRol.equalsIgnoreCase(r.getCodigo()));
  }

  public boolean estaActivo() {
    return Boolean.TRUE.equals(this.activo);
  }

  public void activar() {
    this.activo = true;
    this.fechaActualizacion = LocalDateTime.now();
  }

  public void desactivar() {
    this.activo = false;
    this.fechaActualizacion = LocalDateTime.now();
  }

  public void cambiarPassword(String nuevoPasswordHash) {
    this.usuarioPassword = nuevoPasswordHash;
    this.fechaActualizacion = LocalDateTime.now();
  }

  public void marcarCambioPasswordInicialPendiente() {
    this.debeCambiarPassword = true;
  }

  public void completarCambioPasswordInicial() {
    this.debeCambiarPassword = false;
  }

  public void actualizarPerfil(String email) {
    if (email != null && !email.isBlank()) {
      this.email = email;
    }
    this.fechaActualizacion = LocalDateTime.now();
  }

  public void agregarRol(Rol rol) {
    if (this.roles == null) {
      this.roles = new HashSet<>();
    }
    if (rol != null) {
      this.roles.add(rol);
    }
  }

  public void removerRol(Rol rol) {
    if (this.roles != null && rol != null) {
      this.roles.remove(rol);
    }
  }

  public void revocarRol(Rol rol, LocalDateTime ahora) {
    removerRol(rol);
    this.fechaActualizacion = ahora;
  }

  public void corregirDni(String nuevoDni, LocalDateTime ahora) {
    this.dni = nuevoDni;
    this.fechaActualizacion = ahora;
  }

  public void actualizarNombreCompleto(String nombreCompleto, LocalDateTime ahora) {
    if (nombreCompleto == null || nombreCompleto.isBlank()) {
      throw new com.colegio.shuji.shared.domain.exception.BusinessException("El nombre completo es obligatorio");
    }
    this.nombreCompleto = nombreCompleto.trim();
    this.fechaActualizacion = ahora;
  }

  public void registrar(String usuarioPassword, Set<Rol> roles, LocalDateTime ahora) {
    if (id != null
        || usuarioPassword == null
        || usuarioPassword.isBlank()
        || roles == null
        || roles.isEmpty())
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "Registro de usuario inválido");
    this.usuarioPassword = usuarioPassword;
    this.roles = new HashSet<>(roles);
    activo = true;
    fechaCreacion = ahora;
    fechaActualizacion = ahora;
  }
}
