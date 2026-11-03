package com.colegio.shuji.usuario.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de salida con los datos públicos del Usuario (sin hash de contraseña). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

  private Long id;
  private String username;
  private String email;
  private Boolean activo;
  private Boolean debeCambiarPassword;
  private String dni;
  private String telefono;
  private String direccion;
  private String especialidad;
  private String codigoModular;
  private String numeroColegiatura;
  private java.time.LocalDate fechaIngreso;
  private String tipoContrato;
  private String estadoLaboral;
  private Set<RolResponseDto> roles;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("createdAt")
  private LocalDateTime fechaCreacion;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("updatedAt")
  private LocalDateTime fechaActualizacion;
}
