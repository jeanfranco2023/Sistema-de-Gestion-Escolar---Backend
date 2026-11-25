package com.colegio.shuji.usuario.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
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
  private String nombreCompleto;
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
  @Builder.Default private List<AreaDocenteResponseDto> areasCurriculares = List.of();

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("createdAt")
  private LocalDateTime fechaCreacion;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("updatedAt")
  private LocalDateTime fechaActualizacion;

  public UserResponseDto(
      Long id,
      String username,
      String nombreCompleto,
      String email,
      Boolean activo,
      Boolean debeCambiarPassword,
      String dni,
      String telefono,
      String direccion,
      String especialidad,
      String codigoModular,
      String numeroColegiatura,
      java.time.LocalDate fechaIngreso,
      String tipoContrato,
      String estadoLaboral,
      Set<RolResponseDto> roles,
      LocalDateTime fechaCreacion,
      LocalDateTime fechaActualizacion) {
    this(
        id, username, nombreCompleto, email, activo, debeCambiarPassword, dni,
        telefono, direccion, especialidad, codigoModular, numeroColegiatura,
        fechaIngreso, tipoContrato, estadoLaboral, roles, List.of(),
        fechaCreacion, fechaActualizacion);
  }
}
