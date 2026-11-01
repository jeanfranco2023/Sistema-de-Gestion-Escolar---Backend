package com.colegio.shuji.shared.application.port.out;

public interface ActorActualPort {
  Long usuarioId();

  default String nombreUsuario() {
    return String.valueOf(usuarioId());
  }

  boolean tieneRol(String rol);

  void verificarDocente(Long docenteId);
}
