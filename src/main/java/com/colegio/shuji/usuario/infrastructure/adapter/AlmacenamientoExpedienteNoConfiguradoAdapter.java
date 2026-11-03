package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoStoragePort;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("prod | !local-docker")
public final class AlmacenamientoExpedienteNoConfiguradoAdapter implements ExpedienteEmpleadoStoragePort {

  @Override
  public void guardar(String rutaObjeto, byte[] contenido) {
    throw noDisponible();
  }

  @Override
  public void eliminar(String rutaObjeto) {
    // No hay almacenamiento configurado fuera del perfil local.
  }

  @Override
  public String urlFirmada(String rutaObjeto) {
    throw noDisponible();
  }

  private static ResponseStatusException noDisponible() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "El almacenamiento de expedientes no está configurado para este perfil");
  }
}
