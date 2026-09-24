package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de entrada para la actualización de perfil y datos básicos del usuario. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsuarioRequestDto {

  @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
  private String nombres;

  @NotBlank(message = "El correo electrónico es obligatorio")
  @Email(message = "El formato del correo electrónico no es válido")
  @Size(max = 254, message = "El correo electrónico no puede superar los 254 caracteres")
  private String email;
}
