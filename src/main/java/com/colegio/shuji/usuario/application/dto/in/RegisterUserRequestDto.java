package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
  @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
  private String username;

  @NotBlank(message = "El correo electrónico es obligatorio")
  @Email(message = "El formato del correo electrónico no es válido")
  @Size(max = 254, message = "El correo electrónico no puede superar los 254 caracteres")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
  private String password;

  @NotEmpty(message = "Debe asignar al menos un rol al usuario")
  private Set<String> roles;
}
