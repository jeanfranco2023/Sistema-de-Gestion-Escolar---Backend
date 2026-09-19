package com.colegio.shuji.matricula.application.port.out;

import java.util.Optional;

public interface ReniecServicePort {
  record Identidad(
      String numeroDocumento, String nombres, String apellidoPaterno, String apellidoMaterno) {}

  Optional<Identidad> consultar(String dni);
}
