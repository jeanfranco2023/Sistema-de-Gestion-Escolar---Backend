package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.AlmacenJustificacionPort;
import com.colegio.shuji.shared.infrastructure.storage.LocalPrivateFileStore;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("local-docker & !prod")
public class LocalStorageJustificacionAdapter implements AlmacenJustificacionPort {
  private static final String BUCKET = "asistencia-justificaciones";

  private final LocalPrivateFileStore storage;

  public LocalStorageJustificacionAdapter(LocalPrivateFileStore storage) {
    this.storage = storage;
  }

  @Override
  public Archivo guardar(Long asistenciaId, byte[] contenido) {
    if (asistenciaId == null || asistenciaId < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asistencia no válida");
    }
    String key = "asistencia/justificaciones/" + asistenciaId + "/" + UUID.randomUUID() + ".pdf";
    storage.guardar(BUCKET, key, contenido);
    return new Archivo(key);
  }

  @Override
  public String urlFirmada(String clave) {
    return storage.urlFirmada(BUCKET, clave);
  }

  @Override
  public void eliminar(String clave) {
    storage.eliminar(BUCKET, clave);
  }
}
