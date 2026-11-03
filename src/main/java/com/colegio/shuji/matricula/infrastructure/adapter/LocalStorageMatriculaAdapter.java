package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.shared.infrastructure.storage.LocalPrivateFileStore;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("local-docker & !prod")
public class LocalStorageMatriculaAdapter implements AlmacenDocumentosMatriculaPort {
  private static final String BUCKET = "matricula-documentos";

  private final LocalPrivateFileStore storage;

  public LocalStorageMatriculaAdapter(LocalPrivateFileStore storage) {
    this.storage = storage;
  }

  @Override
  public void validarConfiguracion() {
    storage.validarConfiguracion();
  }

  @Override
  public ArchivoGuardado guardar(UUID solicitudId, TipoDocumentoSolicitud tipo, byte[] contenido, String mimeType) {
    String extension = switch (mimeType) {
      case "application/pdf" -> "pdf";
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de documento no permitido");
    };
    String key = solicitudId + "/" + tipo.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + "." + extension;
    storage.guardar(BUCKET, key, contenido);
    return new ArchivoGuardado(BUCKET, key);
  }

  @Override
  public String urlFirmada(String bucket, String objectKey) {
    if (!BUCKET.equals(bucket)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no disponible");
    return storage.urlFirmada(bucket, objectKey);
  }

  @Override
  public void eliminar(ArchivoGuardado archivo) {
    if (archivo != null && BUCKET.equals(archivo.bucket())) storage.eliminar(BUCKET, archivo.objectKey());
  }
}
