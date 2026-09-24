package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SupabaseStorageMatriculaAdapter implements AlmacenDocumentosMatriculaPort {
  private final String url;
  private final String serviceRoleKey;
  private final String bucket;
  private final int signedUrlSeconds;
  private final RestClient client;

  public SupabaseStorageMatriculaAdapter(
      @Value("${supabase.storage.url:}") String url,
      @Value("${supabase.storage.service-role-key:}") String serviceRoleKey,
      @Value("${supabase.storage.bucket:matricula-documentos}") String bucket,
      @Value("${supabase.storage.signed-url-seconds:60}") int signedUrlSeconds) {
    this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
    this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey.trim();
    this.bucket = bucket == null ? "matricula-documentos" : bucket.trim();
    this.signedUrlSeconds = Math.max(30, Math.min(600, signedUrlSeconds));
    this.client = RestClient.builder().build();
  }

  @Override
  public void validarConfiguracion() {
    if (url.isBlank() || serviceRoleKey.isBlank() || bucket.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El almacenamiento privado de documentos aún no está configurado");
    }
  }

  @Override
  public ArchivoGuardado guardar(UUID solicitudId, TipoDocumentoSolicitud tipo, byte[] contenido, String mimeType) {
    validarConfiguracion();
    asegurarBucketPrivado();
    String extension = switch (mimeType) {
      case "application/pdf" -> "pdf";
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de documento no permitido");
    };
    String objectKey = solicitudId + "/" + tipo.name().toLowerCase() + "." + extension;
    try {
      client.put()
          .uri(url + "/storage/v1/object/" + bucket + "/" + objectKey)
          .header("apikey", serviceRoleKey)
          .header("Authorization", "Bearer " + serviceRoleKey)
          .header("x-upsert", "true")
          .contentType(MediaType.parseMediaType(mimeType))
          .body(contenido)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "No se pudo guardar el documento en el almacenamiento privado");
    }
    return new ArchivoGuardado(bucket, objectKey);
  }

  @Override
  public String urlFirmada(String bucket, String objectKey) {
    validarConfiguracion();
    asegurarBucketPrivado();
    final JsonNode response;
    try {
      response = client.post()
          .uri(url + "/storage/v1/object/sign/" + bucket + "/" + objectKey)
          .header("apikey", serviceRoleKey)
          .header("Authorization", "Bearer " + serviceRoleKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("expiresIn", signedUrlSeconds))
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "No se pudo generar el enlace privado para el documento");
    }
    String signedPath = response == null ? null : response.path("signedURL").asText(null);
    if (signedPath == null || !signedPath.startsWith("/object/sign/")) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase no devolvió una URL temporal válida");
    }
    return url + "/storage/v1" + signedPath;
  }

  private void asegurarBucketPrivado() {
    final JsonNode bucketInfo;
    try {
      bucketInfo = client.get()
          .uri(url + "/storage/v1/bucket/" + bucket)
          .header("apikey", serviceRoleKey)
          .header("Authorization", "Bearer " + serviceRoleKey)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() != 404) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "No se pudo verificar el bucket privado de documentos en Supabase");
      }
      try {
        JsonNode created = client.post()
            .uri(url + "/storage/v1/bucket")
            .header("apikey", serviceRoleKey)
            .header("Authorization", "Bearer " + serviceRoleKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "id", bucket,
                "name", bucket,
                "public", false,
                "fileSizeLimit", 5L * 1024 * 1024,
                "allowedMimeTypes", List.of("application/pdf", "image/jpeg", "image/png", "image/webp")))
            .retrieve()
            .body(JsonNode.class);
        if (created != null && created.has("public") && !created.path("public").asBoolean(true)) return;
      } catch (RestClientException createError) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "No se pudo crear el bucket privado de documentos en Supabase");
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Supabase no confirmó que el bucket de documentos sea privado");
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo verificar el bucket privado de documentos en Supabase");
    }
    if (bucketInfo == null || !bucketInfo.has("public") || bucketInfo.path("public").asBoolean(true)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El bucket de documentos está ausente o no es privado");
    }
  }
}
