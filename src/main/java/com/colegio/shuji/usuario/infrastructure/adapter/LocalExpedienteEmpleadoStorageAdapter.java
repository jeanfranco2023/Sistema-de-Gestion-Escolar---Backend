package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.shared.infrastructure.storage.LocalPrivateFileStore;
import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoStoragePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-docker & !prod")
public final class LocalExpedienteEmpleadoStorageAdapter implements ExpedienteEmpleadoStoragePort {

  private static final String BUCKET = "expedientes-empleados";

  private final LocalPrivateFileStore storage;

  public LocalExpedienteEmpleadoStorageAdapter(LocalPrivateFileStore storage) {
    this.storage = storage;
  }

  @Override
  public void guardar(String rutaObjeto, byte[] contenido) {
    storage.guardar(BUCKET, rutaObjeto, contenido);
  }

  @Override
  public void eliminar(String rutaObjeto) {
    storage.eliminar(BUCKET, rutaObjeto);
  }

  @Override
  public String urlFirmada(String rutaObjeto) {
    return storage.urlFirmada(BUCKET, rutaObjeto);
  }
}
