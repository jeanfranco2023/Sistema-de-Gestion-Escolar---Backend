package com.colegio.shuji.academico.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aulas")
@Getter
@Setter
@NoArgsConstructor
public class AulaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false, unique = true, length = 30)
  private String codigo;

  @Column(nullable = false, length = 40)
  private String nombre;

  @Column(length = 150)
  private String ubicacion;

  @Column(nullable = false)
  private Short capacidad;

  @Column(nullable = false)
  private Boolean activa;
}
