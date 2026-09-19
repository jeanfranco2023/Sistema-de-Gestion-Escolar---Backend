package com.colegio.shuji.usuario.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Modelo puro del Dominio que representa un Rol en el sistema. Desacoplado de la persistencia (sin
 * anotaciones de JPA).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
public class Rol {

  private Short id;
  private String codigo;
  private String nombre;
  private String descripcion;
}
