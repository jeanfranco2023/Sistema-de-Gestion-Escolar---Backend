package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de entrada para el registro o creación de un nuevo usuario en la plataforma. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequestDto {

  @NotBlank(message = "El nombre de usuario es obligatorio")
  @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
  private String username;

  @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente 8 dígitos numéricos")
  private String dni;

  @Size(max = 200, message = "El nombre completo no puede superar los 200 caracteres")
  private String nombreCompleto;

  @NotBlank(message = "El correo electrónico es obligatorio")
  @Email(message = "El formato del correo electrónico no es válido")
  @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres")
  private String email;

  // Contraseña opcional para creación administrativa (se genera temporal si se omite)
  @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
  private String password;

  @NotEmpty(message = "Debe asignar al menos un rol al usuario")
  private Set<String> roles;
}
