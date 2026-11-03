package com.colegio.shuji.usuario.application.port.out;

/** Almacenamiento privado de archivos del expediente laboral. */
public interface ExpedienteEmpleadoStoragePort {

  void guardar(String rutaObjeto, byte[] contenido);

  void eliminar(String rutaObjeto);

  String urlFirmada(String rutaObjeto);
}
