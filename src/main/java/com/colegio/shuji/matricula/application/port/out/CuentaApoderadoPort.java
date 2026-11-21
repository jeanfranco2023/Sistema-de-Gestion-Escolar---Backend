package com.colegio.shuji.matricula.application.port.out;

/**
 * Puerto de salida para el aprovisionamiento y gestión de cuentas de usuario para apoderados
 * al confirmarse su matrícula.
 */
public interface CuentaApoderadoPort {

  record CredencialesApoderado(
      Long usuarioId,
      String username,
      String email,
      String passwordTemporal,
      boolean esNuevaCuenta
  ) {}

  CredencialesApoderado asegurarCuentaApoderado(
      String numeroDocumento,
      String email,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      String telefono,
      String direccion
  );
}
