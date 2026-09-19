package com.colegio.shuji.usuario.domain.enums;

/**
 * Catálogo institucional de roles para el control de acceso basado en roles (RBAC). Alineado a las
 * restricciones de la base de datos de la I.E.P. Shuji Kitamura.
 */
public enum RolNombre {
  DIRECCION("Dirección General / Directiva"),
  SECRETARIA("Secretaría Académica"),
  DOCENTE("Docente de Asignatura"),
  AUXILIAR("Auxiliar de Disciplina y Asistencia"),
  TUTOR("Tutor de Aula"),
  APODERADO("Padre de Familia o Apoderado");

  private final String descripcion;

  RolNombre(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getDescripcion() {
    return descripcion;
  }
}
